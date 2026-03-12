package op.edu.ua.petbed.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class FixTelegramBotMapping implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {

        converters.add(new ByteArrayHttpMessageConverter()); // 2026-03-12T19:38:20.363Z  WARN 1 --- [pet-bed] [nio-8080-exec-2] .w.s.m.s.DefaultHandlerExceptionResolver : Resolved [org.springframework.http.converter.HttpMessageNotWritableException: No converter for [class [B] with preset Content-Type 'application/openmetrics-text;version=1.0.0;charset=utf-8'] 2026-03-12T19:38:25.364853058Z 20)
        com.fasterxml.jackson.databind.ObjectMapper fasterxmlMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        converters.add(0, new MappingJackson2HttpMessageConverter(fasterxmlMapper));
    }
}
