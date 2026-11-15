package com.shortener.kgs;

import com.shortener.common.KeyScrambler;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KgsConfig {

    @Bean(initMethod = "start", destroyMethod = "close")
    CuratorFramework curator(@Value("${kgs.zookeeper.connect}") String connect) {
        return CuratorFrameworkFactory.newClient(connect, new ExponentialBackoffRetry(200, 5));
    }

    /** The seed is a secret. Changing it after keys were issued breaks the one-to-one guarantee. */
    @Bean
    KeyScrambler keyScrambler(@Value("${kgs.scrambler-seed}") long seed) {
        return new KeyScrambler(seed);
    }
}
