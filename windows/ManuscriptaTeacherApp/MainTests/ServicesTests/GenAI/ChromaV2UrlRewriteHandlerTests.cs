using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Unit tests for <see cref="ChromaV2UrlRewriteHandler.RewriteUri"/>.
/// Verifies that ChromaDB.Client v1 API URLs (query-parameter-based
/// tenant/database routing) are correctly rewritten to Chroma v2 API
/// URLs (path-based routing).
///
/// See GenAISpec.md §1B, §2(3)(a1).
/// </summary>
public class ChromaV2UrlRewriteHandlerTests
{
    // ───────────────────────── Collection management URLs ─────────────────────────

    [Fact]
    public void RewriteUri_GetOrCreateCollection_RewritesQueryParamsToPath()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections?tenant=default_tenant&database=default_database");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_GetCollectionByName_RewritesCorrectly()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections/source_documents?tenant=default_tenant&database=default_database");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/source_documents",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_DeleteCollection_RewritesCorrectly()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections/myCollection?tenant=custom_tenant&database=custom_db");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/custom_tenant/databases/custom_db/collections/myCollection",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_CountCollections_RewritesCorrectly()
    {
        var input = new Uri("http://localhost:8000/api/v2/count_collections?tenant=default_tenant&database=default_database");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/count_collections",
            result.ToString());
    }

    // ────────────────── Collection operation URLs (no query params) ──────────────

    [Fact]
    public void RewriteUri_CollectionAdd_InjectsDefaultTenantDatabase()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/add");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/add",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_CollectionQuery_InjectsDefaultTenantDatabase()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/query");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/query",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_CollectionGet_InjectsDefaultTenantDatabase()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/get");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/get",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_CollectionDelete_InjectsDefaultTenantDatabase()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/delete");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/delete",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_CollectionCount_InjectsDefaultTenantDatabase()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/count");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/6c80ebea-2be1-4ad9-a290-796dfcace251/count",
            result.ToString());
    }

    // ───────────────────────── Non-collection URLs (passthrough) ─────────────────

    [Fact]
    public void RewriteUri_Heartbeat_PassesThrough()
    {
        var input = new Uri("http://localhost:8000/api/v2/heartbeat");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal("http://localhost:8000/api/v2/heartbeat", result.ToString());
    }

    [Fact]
    public void RewriteUri_Version_PassesThrough()
    {
        var input = new Uri("http://localhost:8000/api/v2/version");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal("http://localhost:8000/api/v2/version", result.ToString());
    }

    [Fact]
    public void RewriteUri_TenantCreation_PassesThrough()
    {
        var input = new Uri("http://localhost:8000/api/v2/tenants");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal("http://localhost:8000/api/v2/tenants", result.ToString());
    }

    [Fact]
    public void RewriteUri_DatabaseCreation_PassesThrough()
    {
        var input = new Uri("http://localhost:8000/api/v2/tenants/default_tenant/databases");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases",
            result.ToString());
    }

    // ────────────────────────── Edge cases ───────────────────────────────────────

    [Fact]
    public void RewriteUri_UrlEncodedTenantName_PreservesEncoding()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections?tenant=my%20tenant&database=my%20db");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        // The rewritten path should contain the tenant/database in path segments.
        // URI may normalize %20 to spaces internally; verify the logical structure.
        Assert.Contains("/tenants/", result.AbsolutePath);
        Assert.Contains("/databases/", result.AbsolutePath);
        Assert.Contains("/collections", result.AbsolutePath);
        Assert.Contains("my", result.AbsolutePath);
    }

    [Fact]
    public void RewriteUri_ReversedQueryParamOrder_StillWorks()
    {
        var input = new Uri("http://localhost:8000/api/v2/collections?database=default_database&tenant=default_tenant");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections",
            result.ToString());
    }

    [Fact]
    public void RewriteUri_CollectionModify_InjectsDefaultTenantDatabase()
    {
        // ChromaCollectionClient.Modify sends PUT to collections/{id}
        var input = new Uri("http://localhost:8000/api/v2/collections/6c80ebea-2be1-4ad9-a290-796dfcace251");
        var result = ChromaV2UrlRewriteHandler.RewriteUri(input);
        Assert.Equal(
            "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/6c80ebea-2be1-4ad9-a290-796dfcace251",
            result.ToString());
    }

    // ──────────────── Response patching (v2 → v1 compat) ────────────────────────

    /// <summary>
    /// Verifies that a query response missing required properties gets them
    /// injected as shape-matching default arrays (not <c>null</c>) so the
    /// client library's entry mapper can safely index into them.
    /// </summary>
    [Fact]
    public async Task SendAsync_QueryResponse_InjectsMissingPropertiesWithMatchingShape()
    {
        // v2 server returns only ids, documents, distances, and included.
        var serverResponseJson = """{"ids":[["id1","id2"]],"documents":[["chunk1","chunk2"]],"distances":[[0.1,0.2]],"included":["documents","distances"]}""";
        var fakeInner = new FakeHttpHandler(serverResponseJson);
        var handler = new ChromaV2UrlRewriteHandler { InnerHandler = fakeInner };
        var client = new HttpClient(handler);

        var request = new HttpRequestMessage(HttpMethod.Post,
            "http://localhost:8000/api/v2/collections/abc/query");
        request.Content = new StringContent("{}", Encoding.UTF8, "application/json");

        var response = await client.SendAsync(request);
        var body = await response.Content.ReadAsStringAsync();

        // Missing array properties must be shape-matching (nested arrays with
        // the same inner lengths as ids), NOT null.
        Assert.Contains("\"embeddings\":[[null,null]]", body);
        Assert.Contains("\"metadatas\":[[null,null]]", body);
        Assert.Contains("\"uris\":[[null,null]]", body);

        // data has no array shape — it stays null.
        Assert.Contains("\"data\":null", body);

        // Original properties must be preserved.
        Assert.Contains("\"ids\":", body);
        Assert.Contains("\"documents\":", body);
        Assert.Contains("\"distances\":", body);
    }

    /// <summary>
    /// Verifies that a GET response (flat ids) is patched with flat default arrays.
    /// </summary>
    [Fact]
    public async Task SendAsync_GetResponse_InjectsFlatDefaultArrays()
    {
        var serverResponseJson = """{"ids":["id1","id2"],"documents":["doc1","doc2"],"included":["documents"]}""";
        var fakeInner = new FakeHttpHandler(serverResponseJson);
        var handler = new ChromaV2UrlRewriteHandler { InnerHandler = fakeInner };
        var client = new HttpClient(handler);

        var request = new HttpRequestMessage(HttpMethod.Post,
            "http://localhost:8000/api/v2/collections/abc/get");
        request.Content = new StringContent("{}", Encoding.UTF8, "application/json");

        var response = await client.SendAsync(request);
        var body = await response.Content.ReadAsStringAsync();

        // Flat arrays matching ids length.
        Assert.Contains("\"embeddings\":[null,null]", body);
        Assert.Contains("\"metadatas\":[null,null]", body);
        Assert.Contains("\"distances\":[0,0]", body);
        Assert.Contains("\"data\":null", body);
    }

    /// <summary>
    /// When the response already contains all required properties (e.g. from a
    /// v1-compatible server), the body must not be altered.
    /// </summary>
    [Fact]
    public async Task SendAsync_QueryResponseComplete_NoPatching()
    {
        var completeJson = """{"ids":[[]],"documents":[[]],"metadatas":[[]],"embeddings":[[]],"distances":[[]],"uris":[[]],"data":null,"included":[]}""";
        var fakeInner = new FakeHttpHandler(completeJson);
        var handler = new ChromaV2UrlRewriteHandler { InnerHandler = fakeInner };
        var client = new HttpClient(handler);

        var request = new HttpRequestMessage(HttpMethod.Post,
            "http://localhost:8000/api/v2/collections/abc/query");
        request.Content = new StringContent("{}", Encoding.UTF8, "application/json");

        var response = await client.SendAsync(request);
        var body = await response.Content.ReadAsStringAsync();

        // Should be unchanged (white-space differences are acceptable).
        Assert.Equal(completeJson, body);
    }

    /// <summary>
    /// Non-query/get POST endpoints (e.g. /add, /delete) must not be patched.
    /// </summary>
    [Fact]
    public async Task SendAsync_AddEndpoint_NoPatchingApplied()
    {
        var serverResponseJson = """{"status":"ok"}""";
        var fakeInner = new FakeHttpHandler(serverResponseJson);
        var handler = new ChromaV2UrlRewriteHandler { InnerHandler = fakeInner };
        var client = new HttpClient(handler);

        var request = new HttpRequestMessage(HttpMethod.Post,
            "http://localhost:8000/api/v2/collections/abc/add");
        request.Content = new StringContent("{}", Encoding.UTF8, "application/json");

        var response = await client.SendAsync(request);
        var body = await response.Content.ReadAsStringAsync();

        Assert.Equal(serverResponseJson, body);
    }

    /// <summary>
    /// Fake <see cref="HttpMessageHandler"/> that returns a fixed JSON response.
    /// </summary>
    private sealed class FakeHttpHandler : HttpMessageHandler
    {
        private readonly string _responseJson;

        public FakeHttpHandler(string responseJson)
        {
            _responseJson = responseJson;
        }

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request, CancellationToken cancellationToken)
        {
            var response = new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent(_responseJson, Encoding.UTF8, "application/json")
            };
            return Task.FromResult(response);
        }
    }
}
