using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TestNetworkProtocol : MonoBehaviour
{
    private readonly TcpClientManager tcpClientManager = new TcpClientManager();
    private readonly UdpClientManager udpClientManager = new UdpClientManager();

    // Use this for initialization
    void Start()
    {
        // tcpClientManager.Connect();
        udpClientManager.Connect();
        udpClientManager.SendClientMessage("hello world");
    }

    void OnDisable()
    {
        // tcpClientManager.Disconnet();
        udpClientManager.Disconnet();
    }
}
