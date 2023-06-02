package com.github.vincentrussell.query.mongodb.sql.converter.rule;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

public class MongoRule extends ExternalResource {

    private final IFeatureAwareVersion version;
    private MongodStarter starter = MongodStarter.getDefaultInstance();
    private MongodProcess mongodProcess;
    private MongodExecutable mongodExecutable;
    private int port = getRandomFreePort();
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private MongoCollection mongoCollection;


    public MongoRule(IFeatureAwareVersion version) {
        this.version = version;
    }

    @Override
    protected void before() throws Throwable {
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(version)
                .cmdOptions(new MongoCmdOptionsBuilder()
                        .useNoPrealloc(false)
                        .useSmallFiles(false)
                        .build())
                .net(new Net("localhost",port, false))
                .build();

        mongodExecutable = starter.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();
        mongoClient = new MongoClient("localhost",port);
    }

    public MongoDatabase getDatabase(String databaseName) {
        return mongoClient.getDatabase(databaseName);
    }

    private static int getRandomFreePort() {
        Random r = new Random();
        int count = 0;

        while (count < 13) {
            int port = r.nextInt((1 << 16) - 1024) + 1024;

            ServerSocket so = null;
            try {
                so = new ServerSocket(port);
                so.setReuseAddress(true);
                return port;
            } catch (IOException ioe) {

            } finally {
                if (so != null)
                    try {
                        so.close();
                    } catch (IOException e) {}
            }

        }

        throw new RuntimeException("Unable to find port");
    }

    @Override
    protected void after() {
        mongoClient.close();
        mongodProcess.stop();
        mongodExecutable.stop();
    }


}
