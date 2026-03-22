package tms.testsupport;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 仅用于 Controller 层 Web 测试：放在主应用 {@code com.test.tms} 扫描包之外，
 * 避免被 {@link com.test.tms.TmsApplication} 误扫描，从而全局排除数据源自动配置。
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class
})
@ComponentScan(basePackages = "com.test.tms.controller")
public class ControllerTestApplication {
}
