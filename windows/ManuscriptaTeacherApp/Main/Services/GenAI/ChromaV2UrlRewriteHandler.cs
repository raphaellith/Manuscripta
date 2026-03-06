using System.Net;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace Main.Services.GenAI;

/// <summary>
/// A <see cref="DelegatingHandler"/> that bridges the ChromaDB.Client v1.0.0
/// NuGet package with the Chroma v2 REST API server.
///
/// <para><strong>Request rewriting</strong>: translates v1 API URLs
/// (query-parameter-based tenant/database routing) into v2 API URLs
/// (path-based routing).</para>
///
/// <para><strong>Response patching</strong>: the v2 server only returns the
/// fields listed in the request's <c>include</c> array, but
/// <c>CollectionEntriesQueryResponse</c> / <c>CollectionEntriesGetResponse</c>
/// in the client library mark every property as <c>required</c>.  This handler
/// injects <c>null</c> for any missing top-level properties so the client's
/// JSON deserializer does not throw.</para>
///
/// See GenAISpec.md §2(3)(a1).
/// </summary>
public sealed class ChromaV2UrlRewriteHandler : DelegatingHandler
{
    private const string DefaultTenant = "default_tenant";
    private const string DefaultDatabase = "default_database";

    // Matches paths that start with "collections" and may carry tenant/database
    // as query parameters (ChromaClient requests).
    //   e.g. collections?tenant=X&database=Y
    //        collections/myCol?tenant=X&database=Y
    //        count_collections?tenant=X&database=Y
    private static readonly Regex TenantDbQueryRegex = new(
        @"[?&]tenant=(?<tenant>[^&]+)",
        RegexOptions.Compiled);

    private static readonly Regex DatabaseQueryRegex = new(
        @"[?&]database=(?<database>[^&]+)",
        RegexOptions.Compiled);

    // Top-level properties that CollectionEntriesQueryResponse / CollectionEntriesGetResponse
    // require but the v2 server omits when they are not in the `include` list.
    private static readonly string[] PatchableProperties =
        ["ids", "embeddings", "metadatas", "documents", "distances", "uris", "data", "included"];

    /// <inheritdoc />
    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        if (request.RequestUri is not null)
        {
            request.RequestUri = RewriteUri(request.RequestUri);
        }

        var response = await base.SendAsync(request, cancellationToken);

        // Patch query/get responses that are missing required properties.
        if (response.IsSuccessStatusCode
            && request.Method == HttpMethod.Post
            && request.RequestUri?.AbsolutePath is { } path
            && (path.EndsWith("/query") || path.EndsWith("/get")))
        {
            response = await PatchResponseAsync(response);
        }

        return response;
    }

    /// <summary>
    /// Reads the JSON response body, adds default values for any missing
    /// top-level properties expected by the client library, and returns
    /// a new <see cref="HttpResponseMessage"/> with the patched body.
    ///
    /// Array-typed properties are filled with shape-matching empty arrays
    /// derived from <c>ids</c> so that the library's entry mapper can
    /// safely index into them without throwing <see cref="NullReferenceException"/>.
    /// </summary>
    private static async Task<HttpResponseMessage> PatchResponseAsync(HttpResponseMessage response)
    {
        var json = await response.Content.ReadAsStringAsync();

        using var doc = JsonDocument.Parse(json);
        if (doc.RootElement.ValueKind != JsonValueKind.Object)
        {
            return response;
        }

        // Check whether any expected property is missing.
        bool needsPatch = false;
        foreach (var prop in PatchableProperties)
        {
            if (!doc.RootElement.TryGetProperty(prop, out _))
            {
                needsPatch = true;
                break;
            }
        }

        if (!needsPatch)
        {
            return response;
        }

        // Determine whether this is a query response (nested ids: [[...]]) or
        // a get response (flat ids: [...]).  Query responses need doubly-nested
        // default arrays; get responses need singly-nested ones.
        bool isQueryResponse = false;
        int[] innerLengths = [];
        int flatLength = 0;

        if (doc.RootElement.TryGetProperty("ids", out var idsElement)
            && idsElement.ValueKind == JsonValueKind.Array)
        {
            // Peek at the first child to determine nesting.
            bool nested = false;
            foreach (var child in idsElement.EnumerateArray())
            {
                if (child.ValueKind == JsonValueKind.Array)
                {
                    nested = true;
                }
                break;
            }

            if (nested)
            {
                isQueryResponse = true;
                innerLengths = idsElement.EnumerateArray()
                    .Select(inner => inner.GetArrayLength())
                    .ToArray();
            }
            else
            {
                flatLength = idsElement.GetArrayLength();
            }
        }

        // Rebuild the JSON with missing properties filled in.
        using var ms = new MemoryStream();
        using (var writer = new Utf8JsonWriter(ms))
        {
            writer.WriteStartObject();

            // Write all existing properties.
            foreach (var prop in doc.RootElement.EnumerateObject())
            {
                prop.WriteTo(writer);
            }

            // Add defaults for missing properties.
            foreach (var prop in PatchableProperties)
            {
                if (doc.RootElement.TryGetProperty(prop, out _))
                {
                    continue;
                }

                if (prop == "data")
                {
                    writer.WriteNull(prop);
                }
                else if (prop == "included")
                {
                    writer.WriteStartArray(prop);
                    writer.WriteEndArray();
                }
                else if (isQueryResponse)
                {
                    // Doubly-nested array matching ids shape, e.g. [[null, null], [null]].
                    WriteNestedDefaultArray(writer, prop, innerLengths);
                }
                else
                {
                    // Flat array matching ids length, e.g. [null, null, null].
                    WriteFlatDefaultArray(writer, prop, flatLength);
                }
            }

            writer.WriteEndObject();
        }

        var patchedJson = System.Text.Encoding.UTF8.GetString(ms.ToArray());
        var newContent = new StringContent(patchedJson, System.Text.Encoding.UTF8, "application/json");

        // Preserve original response metadata.
        var patchedResponse = new HttpResponseMessage(response.StatusCode)
        {
            Content = newContent,
            ReasonPhrase = response.ReasonPhrase,
            RequestMessage = response.RequestMessage,
            Version = response.Version
        };
        foreach (var header in response.Headers)
        {
            patchedResponse.Headers.TryAddWithoutValidation(header.Key, header.Value);
        }

        return patchedResponse;
    }

    /// <summary>
    /// Writes a doubly-nested default array whose shape matches <paramref name="innerLengths"/>.
    /// For <c>distances</c> each element is <c>0</c>; for all others it is <c>null</c>.
    /// </summary>
    private static void WriteNestedDefaultArray(Utf8JsonWriter writer, string propertyName, int[] innerLengths)
    {
        writer.WriteStartArray(propertyName);
        foreach (var len in innerLengths)
        {
            writer.WriteStartArray();
            for (int j = 0; j < len; j++)
            {
                if (propertyName == "distances")
                {
                    writer.WriteNumberValue(0);
                }
                else
                {
                    writer.WriteNullValue();
                }
            }
            writer.WriteEndArray();
        }
        writer.WriteEndArray();
    }

    /// <summary>
    /// Writes a flat default array of length <paramref name="length"/>.
    /// For <c>distances</c> each element is <c>0</c>; for all others it is <c>null</c>.
    /// </summary>
    private static void WriteFlatDefaultArray(Utf8JsonWriter writer, string propertyName, int length)
    {
        writer.WriteStartArray(propertyName);
        for (int i = 0; i < length; i++)
        {
            if (propertyName == "distances")
            {
                writer.WriteNumberValue(0);
            }
            else
            {
                writer.WriteNullValue();
            }
        }
        writer.WriteEndArray();
    }

    /// <summary>
    /// Rewrites a v1-style URI to v2-style by moving tenant/database from
    /// query parameters into the path, and by injecting default tenant/database
    /// for collection-operation requests that carry no query parameters.
    /// </summary>
    public static Uri RewriteUri(Uri original)
    {
        var path = original.AbsolutePath;
        var query = original.Query; // includes leading '?'

        // Extract tenant and database from query string, if present.
        var tenantMatch = TenantDbQueryRegex.Match(query);
        var databaseMatch = DatabaseQueryRegex.Match(query);

        string tenant = tenantMatch.Success
            ? Uri.UnescapeDataString(tenantMatch.Groups["tenant"].Value)
            : DefaultTenant;
        string database = databaseMatch.Success
            ? Uri.UnescapeDataString(databaseMatch.Groups["database"].Value)
            : DefaultDatabase;

        // Remove tenant and database params from query string.
        var cleanedQuery = TenantDbQueryRegex.Replace(query, "");
        cleanedQuery = DatabaseQueryRegex.Replace(cleanedQuery, "");
        // Clean up leftover '?' or leading '&'.
        cleanedQuery = cleanedQuery.TrimStart('?').TrimStart('&');
        if (!string.IsNullOrEmpty(cleanedQuery))
        {
            cleanedQuery = "?" + cleanedQuery;
        }

        // Determine the API base in the path (e.g. "/api/v2/").
        // The library sets HttpClient.BaseAddress to the configured URI
        // (e.g. http://localhost:8000/api/v2/), so paths look like
        // /api/v2/collections... or /api/v2/count_collections...
        // We need to find where the "collections" or "count_collections" segment starts.

        // Find the first occurrence of "collections" in the path that represents
        // the start of a v1-style route.
        var collectionsIndex = FindCollectionsSegmentIndex(path);
        if (collectionsIndex < 0)
        {
            // Not a collections-related request (e.g. heartbeat, version, reset).
            // Return as-is with cleaned query.
            return BuildUri(original, path, cleanedQuery);
        }

        var basePath = path[..collectionsIndex]; // e.g. "/api/v2/"
        var relativePath = path[collectionsIndex..]; // e.g. "collections/ID/add"

        // Rewrite: insert tenants/{tenant}/databases/{database}/ before the
        // relative path.
        var rewrittenPath = $"{basePath}tenants/{Uri.EscapeDataString(tenant)}/databases/{Uri.EscapeDataString(database)}/{relativePath}";

        return BuildUri(original, rewrittenPath, cleanedQuery);
    }

    /// <summary>
    /// Finds the index in <paramref name="path"/> where the v1-style
    /// "collections" or "count_collections" segment begins.
    /// Returns -1 if no such segment exists.
    /// </summary>
    private static int FindCollectionsSegmentIndex(string path)
    {
        // Look for "count_collections" first (it contains "collections" as a substring).
        var idx = path.IndexOf("count_collections", StringComparison.Ordinal);
        if (idx > 0 && path[idx - 1] == '/')
        {
            return idx;
        }

        // Look for "collections" as a path segment (preceded by '/').
        idx = 0;
        while (true)
        {
            idx = path.IndexOf("collections", idx, StringComparison.Ordinal);
            if (idx < 0)
            {
                return -1;
            }

            // Ensure it's a path segment, not a substring of another word.
            if (idx > 0 && path[idx - 1] == '/')
            {
                // Make sure it's not "count_collections" (already handled above).
                if (idx < 6 || path.Substring(idx - 6, 6) != "count_")
                {
                    return idx;
                }
            }

            idx += "collections".Length;
        }
    }

    private static Uri BuildUri(Uri original, string path, string query)
    {
        var builder = new UriBuilder(original)
        {
            Path = path,
            Query = query
        };
        return builder.Uri;
    }
}
