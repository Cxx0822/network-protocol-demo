using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using UnityEngine;

public class UdpClientManager
{
    private readonly string udpServiceIp = "127.0.0.1";
    private readonly int udpClientPort = 6677;
    private Socket clientUdpSocket;

    public string receiveMessage;

    private IPEndPoint serviceEndPoint;
    private EndPoint receiveEndPoint;
    private Thread receivethread;

    public void Connect()
    {
        if (clientUdpSocket != null && clientUdpSocket.Connected)
        {
            Debug.Log("已经连接成功");
            return;
        }

        try
        {
            Debug.Log("开始创建Udp " + udpServiceIp + ":" + udpClientPort.ToString());
            // 创建客户端Socket，获得远程ip和端口号
            clientUdpSocket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
            IPAddress ip = IPAddress.Parse(udpServiceIp);

            serviceEndPoint = new IPEndPoint(ip, udpClientPort);
            receiveEndPoint = new IPEndPoint(IPAddress.Any, 0);
            Debug.Log("创建Udp " + udpServiceIp + ":" + udpClientPort.ToString() + "成功");

            receivethread = new Thread(ReceivedServiceMessage)
            {
                IsBackground = true
            };
            receivethread.Start();
        }
        catch (Exception exception)
        {
            Debug.LogError("创建Udp" + udpServiceIp + ":" + udpClientPort.ToString() + "失败:" + exception.Message);
        }
    }

    /// <summary>
    /// 客户端发送消息
    /// </summary>
    /// <param name="message"></param>
    public void SendClientMessage(string message)
    {
        try
        {
            Debug.Log("客户端发送消息: " + message);
            byte[] sendData = Encoding.UTF8.GetBytes(message);
            //将数据发送到服务端
            clientUdpSocket.SendTo(sendData, sendData.Length, SocketFlags.None, serviceEndPoint);
        }
        catch (Exception exception)
        {
            Debug.LogError("发送消息错误: " + exception.Message);
        }
    }

    /// <summary>
    /// 接收来自服务端的消息
    /// </summary>
    void ReceivedServiceMessage()
    {
        try
        {
            while (true)
            {
                byte[] recvData = new byte[1024];
                int len = clientUdpSocket.ReceiveFrom(recvData, ref receiveEndPoint);
                if (len > 0)
                {
                    receiveMessage = Encoding.UTF8.GetString(recvData, 0, len);
                    Debug.Log("接收服务器消息: " + receiveMessage);
                }
            }
        }
        catch (Exception exception)
        {
            Debug.LogError("消息接收错误: " + exception.Message);
        }
    }

    /// <summary>
    /// 断开Udp连接
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

            // 断开连接
            if (clientUdpSocket != null)
            {
                clientUdpSocket.Close();
                Debug.Log("断开连接");
            }
        }
        catch (Exception exception)
        {
            Debug.LogError("断开连接失败: " + exception.Message);
        }
    }
}