using System;
using System.IO;
using System.Threading.Tasks;
using Main.Services.GenAI;
using Main.Services.RuntimeDependencies;
using Microsoft.Extensions.Logging;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.RuntimeDependencies;

public class ChromaRuntimeDependencyManagerTests
{
	[Fact]
	public void DependencyId_ReturnsChroma()
	{
		var logger = new Mock<ILogger<ChromaRuntimeDependencyManager>>();
		var manager = new ChromaRuntimeDependencyManager(
			logger.Object,
			CreateProviderConfigurationResolver().Object,
			new ChromaClientService());

		Assert.Equal("chroma", manager.DependencyId);
	}

	[Fact]
	public async Task CheckDependencyAvailabilityAsync_ReturnsTrue_WhenResolvedExecutableSupportsVersion()
	{
		// Skip on non-Windows platforms as chroma installation is Windows-specific
		if (!OperatingSystem.IsWindows())
		{
			return;
		}

		var logger = new Mock<ILogger<ChromaRuntimeDependencyManager>>();
		var (executablePath, tempDirectory) = CreateFakeChromaExecutable();

		try
		{
			var manager = new TestableChromaRuntimeDependencyManager(
				logger.Object,
				CreateProviderConfigurationResolver().Object,
				executablePath,
				chromaApiHealthy: true);

			var result = await manager.CheckDependencyAvailabilityAsync();

			Assert.True(result);
		}
		finally
		{
			if (Directory.Exists(tempDirectory))
			{
				Directory.Delete(tempDirectory, true);
			}
		}
	}

	[Fact]
	public async Task CheckDependencyAvailabilityAsync_ReturnsFalse_WhenResolvedExecutableDoesNotExist()
	{
		var logger = new Mock<ILogger<ChromaRuntimeDependencyManager>>();
		var missingPath = Path.Combine(Path.GetTempPath(), Guid.NewGuid().ToString("N"), "chroma.exe");
		var manager = new TestableChromaRuntimeDependencyManager(
			logger.Object,
			CreateProviderConfigurationResolver().Object,
			missingPath,
			chromaApiHealthy: true);

		var result = await manager.CheckDependencyAvailabilityAsync();

		Assert.False(result);
	}

	[Fact]
	public async Task CheckDependencyAvailabilityAsync_ReturnsTrue_WhenChromaApiUnhealthy_ButExecutableExists()
	{
		// Per GenAISpec.md §1B(3)(a): availability is determined solely by `chroma --version`.
		// API health is irrelevant for availability — the dependency is installed even if the server isn't running.
		var logger = new Mock<ILogger<ChromaRuntimeDependencyManager>>();
		var (executablePath, tempDirectory) = CreateFakeChromaExecutable();

		try
		{
			var manager = new TestableChromaRuntimeDependencyManager(
				logger.Object,
				CreateProviderConfigurationResolver().Object,
				executablePath,
				chromaApiHealthy: false);
			var result = await manager.CheckDependencyAvailabilityAsync();
			Assert.True(result);
		}
		finally
		{
			if (Directory.Exists(tempDirectory))
			{
				Directory.Delete(tempDirectory, true);
			}
		}
	}

	private static (string executablePath, string tempDirectory) CreateFakeChromaExecutable()
	{
		var tempDirectory = Path.Combine(Path.GetTempPath(), "chroma-test-" + Guid.NewGuid().ToString("N"));
		Directory.CreateDirectory(tempDirectory);

		if (OperatingSystem.IsWindows())
		{
			var executablePath = Path.Combine(tempDirectory, "chroma-test.cmd");
			File.WriteAllText(
				executablePath,
				"@echo off\r\n" +
				"if \"%1\"==\"--version\" (\r\n" +
				"  echo chroma 1.0.0\r\n" +
				"  exit /b 0\r\n" +
				")\r\n" +
				"exit /b 1\r\n"
			);
			return (executablePath, tempDirectory);
		}
		else
		{
			var executablePath = Path.Combine(tempDirectory, "chroma-test");
			File.WriteAllText(
				executablePath,
				"#!/bin/sh\n" +
				"if [ \"$1\" = \"--version\" ]; then\n" +
				"  echo chroma 1.0.0\n" +
				"  exit 0\n" +
				"fi\n" +
				"exit 1\n"
			);
			File.SetUnixFileMode(
				executablePath,
				UnixFileMode.UserRead | UnixFileMode.UserWrite | UnixFileMode.UserExecute |
				UnixFileMode.GroupRead | UnixFileMode.GroupExecute |
				UnixFileMode.OtherRead | UnixFileMode.OtherExecute
			);
			return (executablePath, tempDirectory);
		}
	}

	[Fact]
	public async Task EnsureRunningAsync_DoesNotStartServer_WhenApiIsHealthy()
	{
		var logger = new Mock<ILogger<ChromaRuntimeDependencyManager>>();
		var manager = new TestableChromaRuntimeDependencyManager(
			logger.Object,
			CreateProviderConfigurationResolver().Object,
			"chroma.exe",
			chromaApiHealthy: true);

		await manager.EnsureRunningAsync();

		Assert.Equal(0, manager.StartServerCallCount);
	}

	[Fact]
	public async Task EnsureRunningAsync_StartsServer_WhenApiIsUnhealthy()
	{
		var logger = new Mock<ILogger<ChromaRuntimeDependencyManager>>();
		var manager = new TestableChromaRuntimeDependencyManager(
			logger.Object,
			CreateProviderConfigurationResolver().Object,
			"chroma.exe",
			chromaApiHealthy: false);

		await manager.EnsureRunningAsync();

		Assert.Equal(1, manager.StartServerCallCount);
	}

	private sealed class TestableChromaRuntimeDependencyManager : ChromaRuntimeDependencyManager
	{
		private readonly string _resolvedExecutablePath;
		private readonly bool _chromaApiHealthy;

		/// <summary>
		/// Tracks the number of times <see cref="StartServerAsync"/> was invoked.
		/// </summary>
		public int StartServerCallCount { get; private set; }

		public TestableChromaRuntimeDependencyManager(
			ILogger<ChromaRuntimeDependencyManager> logger,
			IProviderConfigurationResolver providerConfigurationResolver,
			string resolvedExecutablePath,
			bool chromaApiHealthy)
			: base(logger, providerConfigurationResolver, new ChromaClientService())
		{
			_resolvedExecutablePath = resolvedExecutablePath;
			_chromaApiHealthy = chromaApiHealthy;
		}

		protected override Task<string> ResolveChromaExecutablePathAsync()
		{
			return Task.FromResult(_resolvedExecutablePath);
		}

		protected override Task<bool> CheckChromaApiHealthAsync()
		{
			return Task.FromResult(_chromaApiHealthy);
		}

		protected override Task StartServerAsync()
		{
			StartServerCallCount++;
			return Task.CompletedTask;
		}
	}

	private static Mock<IProviderConfigurationResolver> CreateProviderConfigurationResolver()
	{
		var resolver = new Mock<IProviderConfigurationResolver>();
		resolver
			.Setup(r => r.GetRequiredField("CHROMA_PROVIDER_CONFIG", "InstallerScriptSource"))
			.Returns("https://example.invalid/install.ps1");
		return resolver;
	}
}
