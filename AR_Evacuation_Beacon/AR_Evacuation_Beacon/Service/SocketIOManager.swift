//
//  SocketIOManager.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/12/22.
//

import UIKit
import SocketIO

// MARK: -Socket connection Manager

class SocketIOManager: NSObject {
    
    static let shared = SocketIOManager()
//    var manager = SocketManager(socketURL: URL(string: "http://146.148.59.28:12000")!, config: [.log(true), .compress])
    var manager = SocketManager(socketURL: URL(string: "http://192.168.2.183:5001")!, config: [.log(true), .compress])
    var socket: SocketIOClient!
    
    override init() {
        super.init()
        socket = self.manager.socket(forNamespace: "/")
    }

    func establishConnection() {
        socket.connect()
        print("socket connected😄")
    }
    
    func closeConnection() {
        socket.disconnect()
        print("socket disconnected😄")
    }
   
    func sendLocation(location: String) {
        socket.emit("location", location)
    }
    
    func receivePath(completionHandler: @escaping ([String]) -> Void) {
        socket.on("message") { (dataArr, socketAck) in
            var pathArr = [String]()
            print("Received Path from server via socket😄")
            print(type(of: dataArr))
            let data = dataArr[0] as! NSDictionary
            let path = data["path"] as! [String]
            pathArr += path
            completionHandler(path)
        }
    }
    
}

