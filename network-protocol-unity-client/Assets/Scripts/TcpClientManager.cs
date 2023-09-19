using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using UnityEngine;

/// <summary>
/// TCP客户端管理类 
/// 处理TCP连接 关闭和收发消息等功能
/// </summary>
public class TcpClientManager
{
    private readonly string tcpServiceIp = "127.0.0.1";
    private int tcpServicePort = 6655;
    private Socket clientTcpSocket;
    private string receiveMessage;
    private Thread receivethread;
    private Thread heartThread;
    private readonly char tcpMessageEndFlag = '$';
    private readonly string tcpHeartBeatFlag = "heartbeat";

    /// <summary>
    /// 连接Tcp
    /// </summary>
    public void Connect()
    {
        if (clientTcpSocket != null && clientTcpSocket.Connected)
        {
            Debug.Log("已经连接成功");
            return;
        }

        try
        {
            Debug.Log("开始连接" + tcpServiceIp + ":" + tcpServicePort.ToString());
            // 创建客户端Socket，获得远程ip和端口号
            clientTcpSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            IPAddress ip = IPAddress.Parse(tcpServiceIp);
            IPEndPoint point = new IPEndPoint(ip, tcpServicePort);
            // 开始连接Tcp
            clientTcpSocket.Connect(point);
            Debug.Log("连接" + tcpServiceIp + ":" + tcpServicePort.ToString() + "成功");

            // 接收消息线程
            receivethread = new Thread(ReceivedServiceMessage)
            {
                IsBackground = true
            };
            receivethread.Start();

            // 发送心跳消息线程
            heartThread = new Thread(SendHeartMessage)
            {
                IsBackground = true
            };
            heartThread.Start();
        }
        catch (Exception exception)
        {
            Debug.LogError("连接" + tcpServiceIp + ":" + tcpServicePort.ToString() + "失败:" + exception.Message);
        }
    }

    /// <summary>
    /// Tcp消息接收
    /// </summary>
    private void ReceivedServiceMessage()
    {
        try
        {
            while (true)
            {
                byte[] buffer = new byte[1024 * 6];
                // 实际接收到的有效字节数
                int len = clientTcpSocket.Receive(buffer);
                if (len == 0)
                {
                    break;
                }

                // 字节解码转为字符串
                receiveMessage = Encoding.UTF8.GetString(buffer, 0, len);

                // 根据分割符切分字符串 解决Tcp粘包问题
                string[] recMesList = receiveMessage.Split(tcpMessageEndFlag);
                foreach (string item in recMesList)
                {
                    ParseServiceMessage(item);
                }
            }

        }
        catch (Exception exception)
        {
            Debug.LogError("消息接收错误: " + exception.Message);
        }
    }

    /// <summary>
    /// 发送心跳消息
    /// </summary>
    private void SendHeartMessage()
    {
        try
        {
            while (true)
            {
                // 每个1.5秒发送心跳信息
                Thread.Sleep(1500);
                // 字符串数据编码转为字节数组
                byte[] sendByte = Encoding.UTF8.GetBytes(tcpHeartBeatFlag + tcpMessageEndFlag);
                // 发送数据至服务器
                clientTcpSocket.Send(sendByte);
            }
        }
        catch (Exception exception)
        {
            Debug.LogError("发送心跳消息错误: " + exception.Message);
        }
    }

    /// <summary>
    /// 客户端发送消息
    /// </summary>
    /// <param name="message">消息</param>
    public void SendClientMessage(string message)
    {
        try
        {
            Debug.Log("客户端发送消息: " + message);
            byte[] buffer = new byte[1024 * 6];
            buffer = Encoding.UTF8.GetBytes(message + tcpMessageEndFlag);
            clientTcpSocket.Send(buffer);
        }
        catch (Exception exception)
        {
            Debug.LogError("发送消息错误: " + exception.Message);
        }
    }

    /// <summary>
    /// 断开Tcp连接
    /// </summary>
    public void Disconnet()
    {
        try
        {
            // 终止线程
            if (receivethread != null)
            {
                receivethread.Interrupt();
                receivethread.Abort();
            }

            if (heartThread != null)
            {
                heartThread.Interrupt();
                heartThread.Abort();
            }

            // 如果Tcp已经连接 则断开连接
            if (clientTcpSocket.Connected)
            {
                // 禁用Socket的发送和接收功能
                clientTcpSocket.Shutdown(SocketShutdown.Both);
                // 关闭Socket连接并释放所有相关资源
                clientTcpSocket.Close();
                Debug.Log("断开连接");
            }
        }
        catch (Exception exception)
        {
            Debug.LogError("断开连接失败: " + exception.Message);
        }
    }

    /// <summary>
    /// 解析Tcp收到的消息
    /// </summary>
    /// <param name="serviceMessage">服务器消息</param>
    private void ParseServiceMessage(string serviceMessage)
    {
        if (string.IsNullOrEmpty(serviceMessage))
        {
            return;
        }

        Debug.Log("接收服务器消息: " + serviceMessage);
    }
}