package com.examly.springapp.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class Aes256ConverterTest {
    @Test
    void roundTripPreservesSensitiveValue() {
        Aes256Converter converter=new Aes256Converter();
        String encrypted=converter.convertToDatabaseColumn("1234567890");
        assertNotEquals("1234567890",encrypted);
        assertEquals("1234567890",converter.convertToEntityAttribute(encrypted));
    }
}
