using System;
using System.IO;
using System.Threading.Tasks;
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
		var manager = new ChromaRuntimeDependencyManager(logger.Object);

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
			var manager = new TestableChromaRuntimeDependencyManager(logger.Object, executablePath, chromaApiHealthy: true);

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
		var manager = new TestableChromaRuntimeDependencyManager(logger.Object, missingPath, chromaApiHealthy: true);

		var result = await manager.CheckDependencyAvailabilityAsync();

		Assert.False(result);
	}

	[Fact]
	public async Task CheckDependencyAvailabilityAsync_ReturnsFalse_WhenChromaApiUnhealthy()
	{
		var logger = new Mock<ILogger<ChromaRuntimeDependencyManager>>();
		var (executablePath, tempDirectory) = CreateFakeChromaExecutable();

		try
		{
			var manager = new TestableChromaRuntimeDependencyManager(logger.Object, executablePath, chromaApiHealthy: false);
			var result = await manager.CheckDependencyAvailabilityAsync();
			Assert.False(result);
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

	private sealed class TestableChromaRuntimeDependencyManager : ChromaRuntimeDependencyManager
	{
		private readonly string _resolvedExecutablePath;
		private readonly bool _chromaApiHealthy;

		public TestableChromaRuntimeDependencyManager(
			ILogger<ChromaRuntimeDependencyManager> logger,
			string resolvedExecutablePath,
			bool chromaApiHealthy)
			: base(logger)
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
	}
}
