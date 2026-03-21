package com.test.tms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

@SpringBootTest
class TmsApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(dataSource, "DataSource should be created");
    }

}
