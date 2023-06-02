package com.github.alexcojocaru.mojo.elasticsearch.v2.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.SystemUtils;

import com.github.alexcojocaru.mojo.elasticsearch.v2.InstanceConfiguration;

/**
 * Several utilities to help in dealing with the elasticsearch file structure.
 * 
 * @author Alex Cojocaru
 */
public final class FilesystemUtil
{

    private FilesystemUtil()
    {
        // hide constructor
    }

    public static File getBinDirectory(File base)
    {
        return new File(base, "/bin/");
    }

    public static File getDataDirectory(File base)
    {
        return new File(base, "/data/");
    }

    public static File getLogsDirectory(File base)
    {
        return new File(base, "/logs/");
    }
    
    public static File getTempDirectory()
    {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Copy a directory recursively, preserving attributes, in particular permissions.
     * @param source The source directory
     * @param destination The destination directory
     * @throws IOException if an IO error occurs
     */
    public static void copyRecursively(Path source, Path destination) throws IOException {
        if (Files.isDirectory(source))
        {
            Files.createDirectories(destination);
            final Set<Path> sources = listFiles(source);

            for (Path srcFile : sources)
            {
                Path destFile = destination.resolve(srcFile.getFileName());
                copyRecursively(srcFile, destFile);
            }
        }
        else
        {
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Set<Path> listFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory))
        {
            return stream.collect(Collectors.toSet());
        }
    }

    /**
     * Set the 755 permissions on the given script.
     * @param config - the instance config
     * @param scriptName - the name of the script (located in the bin directory) to make executable
     */
    public static void setScriptPermission(InstanceConfiguration config, String scriptName)
    {
        if (SystemUtils.IS_OS_WINDOWS)
        {
            // we do not have file permissions on windows
            return;
        }
        if (VersionUtil.isEqualOrGreater_7_0_0(config.getClusterConfiguration().getVersion())) {
            // ES7 and above is packaged as a .tar.gz, and as such the permissions are preserved
            return;
        }
        
        CommandLine command = new CommandLine("chmod")
                .addArgument("755")
                .addArgument(String.format("bin/%s", scriptName));
        ProcessUtil.executeScript(config, command);

        command = new CommandLine("sed")
                .addArguments("-i''")
                .addArgument("-e")
                .addArgument("1s:.*:#!/usr/bin/env bash:", false)
                .addArgument(String.format("bin/%s", scriptName));
        ProcessUtil.executeScript(config, command);
    }
}
