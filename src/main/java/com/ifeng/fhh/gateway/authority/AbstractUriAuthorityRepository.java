package com.ifeng.fhh.gateway.authority;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-5
 */
public abstract class AbstractUriAuthorityRepository {


    public abstract String matchRoleId(String serverId, String uri);
}
