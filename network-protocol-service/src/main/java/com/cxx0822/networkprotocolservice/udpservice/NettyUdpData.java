package com.cxx0822.networkprotocolservice.udpservice;

import lombok.Data;

/**
 * @Author: Cxx
 * @Date: 2023/9/13 22:18
 * @Description: nettyUdp数据
 */
@Data
public class NettyUdpData {

    private String address;

    private String content;

    private long timeStamp;
}
