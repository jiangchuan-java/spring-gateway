package com.ifeng.fhh.gateway.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * jackson工具
 * <p>
 * Created by licheng1 on 2016/8/15.
 */
public class JackSonUtils {


    private static final ObjectMapper ESCAPENULL_MAPPER = new ObjectMapper();

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private static final ObjectMapper UTC_DATE_MAPPER = new ObjectMapper();


    static {
        /*escape null value false*/
        DEFAULT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        /*escape null value true*/
        ESCAPENULL_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ESCAPENULL_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        /*escape null value true UTC date*/
        UTC_DATE_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UTC_DATE_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    public static String clean(String json) {
        return json.replaceAll("[\\r\\n\\t]", "");
    }

    /**
     * json字符串转pojo
     *
     * @param json  json字符串
     * @param clazz pojo类对象
     * @param <T>   pojo类型
     * @return pojo对象
     */
    public static <T> T json2Bean(String json, Class<T> clazz) throws IOException {
        return ESCAPENULL_MAPPER.readValue(clean(json), clazz);
    }

    /**
     * json字符串转pojo(时间为UTC时间)
     *
     * @param json  json字符串
     * @param clazz pojo类对象
     * @param <T>   pojo类型
     * @return pojo对象
     */
    public static <T> T json2BeanUTCDate(String json, Class<T> clazz) throws IOException {
        return UTC_DATE_MAPPER.readValue(clean(json), clazz);
    }

    /**
     * json字符串转pojo集合List
     *
     * @param json  json字符串
     * @param clazz pojo类对象
     * @param <T>   pojo类型
     * @return pojo集合List
     */
    public static <T> List<T> json2List(String json, Class<T> clazz) throws IOException {
        JavaType type = ESCAPENULL_MAPPER.getTypeFactory().constructCollectionLikeType(ArrayList.class, clazz);
        return ESCAPENULL_MAPPER.readValue(clean(json), type);
    }

    /**
     * json字符串转map集合List
     *
     * @param json       json字符串
     * @param keyClass   key类对象
     * @param valueClass value类对象
     * @param <T1>       key类型
     * @param <T2>       value类型
     * @return Map<T1, T2>
     */
    public static <T1, T2> List<Map<T1, T2>> json2MapList(String json, Class<T1> keyClass, Class<T2> valueClass) throws IOException {
        JavaType mapType = ESCAPENULL_MAPPER.getTypeFactory().constructMapType(HashMap.class, keyClass, valueClass);
        JavaType listType = ESCAPENULL_MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, mapType);

        return ESCAPENULL_MAPPER.readValue(clean(json), listType);
    }

    /**
     * json字符串转Map
     *
     * @param json       json字符串
     * @param keyClass   key类对象
     * @param valueClass value类对象
     * @param <T1>       key类型
     * @param <T2>       value类型
     * @return Map<T1, T2>
     */
    public static <T1, T2> Map<T1, T2> json2Map(String json, Class<T1> keyClass, Class<T2> valueClass) throws IOException {
        JavaType type = ESCAPENULL_MAPPER.getTypeFactory().constructMapType(HashMap.class, keyClass, valueClass);
        return ESCAPENULL_MAPPER.readValue(clean(json), type);
    }

    /**
     * url资源转Map
     *
     * @param url        url资源
     * @param keyClass   key类对象
     * @param valueClass value类对象
     * @param <T1>       key类型
     * @param <T2>       value类型
     * @return Map<T1, T2>
     */
    public static <T1, T2> Map<T1, T2> url2Map(URL url, Class<T1> keyClass, Class<T2> valueClass) throws IOException {
        JavaType type = ESCAPENULL_MAPPER.getTypeFactory().constructMapType(HashMap.class, keyClass, valueClass);
        return ESCAPENULL_MAPPER.readValue(url, type);
    }

    /**
     * json字符串转pojo集合Set
     *
     * @param json  json字符串
     * @param clazz pojo类对象
     * @param <T>   pojo类型
     * @return pojo集合Set
     */
    public static <T> Set<T> json2Set(String json, Class<T> clazz) throws IOException {
        JavaType type = ESCAPENULL_MAPPER.getTypeFactory().constructCollectionLikeType(HashSet.class, clazz);
        return ESCAPENULL_MAPPER.readValue(clean(json), type);
    }

    /**
     * pojo转json串
     *
     * @param object
     * @return
     */
    public static String bean2Json(Object object) throws JsonProcessingException {
        return ESCAPENULL_MAPPER.writeValueAsString(object);
    }

    /**
     * pojo转json串
     *
     * @param object
     * @return
     */
    public static String bean2JsonNonEmpty(Object object) throws JsonProcessingException {
        ESCAPENULL_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return ESCAPENULL_MAPPER.writeValueAsString(object);
    }

    /**
     * pojo转json串(时间为UTC时间)
     *
     * @param object
     * @return
     */
    public static String bean2JsonUTCDate(Object object) throws JsonProcessingException {
        return UTC_DATE_MAPPER.writeValueAsString(object);
    }

    /**
     * pojo转map
     * invoke bean2Map(bean, keyClass, valueClass, true)
     *
     * @param bean       pojo对象
     * @param keyClass   key类对象
     * @param valueClass value类对象
     * @param <T1>       key类型
     * @param <T2>       value类型
     * @return map
     */
    public static <T1, T2> Map<T1, T2> bean2Map(Object bean, Class<T1> keyClass, Class<T2> valueClass) {
        return bean2Map(bean, keyClass, valueClass, true);
    }

    /**
     * pojo转map
     *
     * @param bean       pojo对象
     * @param keyClass   key类对象
     * @param valueClass value类对象
     * @param <T1>       key类型
     * @param <T2>       value类型
     * @return map
     */
    public static <T1, T2> Map<T1, T2> bean2MapUTCDate(Object bean, Class<T1> keyClass, Class<T2> valueClass) {
        JavaType type = UTC_DATE_MAPPER.getTypeFactory().constructMapType(HashMap.class, keyClass, valueClass);
        return UTC_DATE_MAPPER.convertValue(bean, type);
    }

    /**
     * pojo转map
     *
     * @param bean       pojo对象
     * @param keyClass   key类对象
     * @param valueClass value类对象
     * @param escapeNull 是否忽略空值
     * @param <T1>       key类型
     * @param <T2>       value类型
     * @return map
     */
    public static <T1, T2> Map<T1, T2> bean2Map(Object bean, Class<T1> keyClass, Class<T2> valueClass, boolean escapeNull) {
        ObjectMapper mapper = DEFAULT_MAPPER;
        if (escapeNull) {
            mapper = ESCAPENULL_MAPPER;
        }
        JavaType type = mapper.getTypeFactory().constructMapType(HashMap.class, keyClass, valueClass);
        return mapper.convertValue(bean, type);
    }


}
