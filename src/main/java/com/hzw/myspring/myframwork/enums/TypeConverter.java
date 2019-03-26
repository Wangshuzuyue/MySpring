package com.hzw.myspring.myframwork.enums;

/**
 * @Auther: huangzuwang
 * @Date: 2019/3/26 16:50
 * @Description:
 */
public interface TypeConverter<T, S> {

    S convert( T value);
}
