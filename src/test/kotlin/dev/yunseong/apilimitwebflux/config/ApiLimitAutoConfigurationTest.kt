package dev.yunseong.apilimitwebflux.config

import dev.yunseong.apilimitwebflux.domain.ClientIpResolver
import dev.yunseong.apilimitwebflux.domain.HeaderClientIpResolver
import dev.yunseong.apilimitwebflux.domain.RemoteAddressClientIpResolver
import dev.yunseong.apilimitwebflux.filter.ApiLimitFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class ApiLimitAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ApiLimitAutoConfiguration::class.java))

    @Test
    fun `defaults to the socket address so existing setups are unchanged`() {
        runner.run { context ->
            assertThat(context.getBean(ClientIpResolver::class.java))
                .isInstanceOf(RemoteAddressClientIpResolver::class.java)
        }
    }

    @Test
    fun `a configured header switches the resolver`() {
        runner.withPropertyValues("api-limit.client-ip-header=CF-Connecting-IP").run { context ->
            val resolver = context.getBean(ClientIpResolver::class.java)
            assertThat(resolver).isInstanceOf(HeaderClientIpResolver::class.java)
            assertThat((resolver as HeaderClientIpResolver).headerName).isEqualTo("CF-Connecting-IP")
        }
    }

    @Test
    fun `a blank header is treated as unset`() {
        runner.withPropertyValues("api-limit.client-ip-header=").run { context ->
            assertThat(context.getBean(ClientIpResolver::class.java))
                .isInstanceOf(RemoteAddressClientIpResolver::class.java)
        }
    }

    @Test
    fun `an application supplied resolver wins`() {
        runner.withPropertyValues("api-limit.client-ip-header=CF-Connecting-IP")
            .withUserConfiguration(CustomResolverConfig::class.java)
            .run { context ->
                assertThat(context.getBean(ClientIpResolver::class.java))
                    .isSameAs(CustomResolverConfig.CUSTOM)
            }
    }

    @Test
    fun `yaml rules still build the filter`() {
        runner.withPropertyValues(
            "api-limit.rules[0].path=/api/**",
            "api-limit.rules[0].limit=5",
            "api-limit.rules[0].duration=1d",
            "api-limit.rules[0].factor=IP"
        ).run { context ->
            assertThat(context).hasSingleBean(ApiLimitFilter::class.java)
        }
    }

    @Configuration(proxyBeanMethods = false)
    class CustomResolverConfig {
        companion object {
            val CUSTOM = ClientIpResolver { "fixed" }
        }

        @Bean
        fun clientIpResolver(): ClientIpResolver = CUSTOM
    }
}
