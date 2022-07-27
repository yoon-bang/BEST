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
    var manager = SocketManager(socketURL: URL(string: "http://146.148.59.28:12000")!, config: [.log(true), .compress])
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
    
    func receivePath(completionHandler: @escaping ([Position]) -> Void) {
        socket.on("path") { (dataArr, socketAck) in
            var pathArr = [String]()
            print("Received Path from server via socket😄")
            let dd =  dataArr[0] as! String
            do {
                let con = try JSONSerialization.jsonObject(with: dd.data(using: .utf8)!, options: []) as! [String:Any]
                let nsPathArr = con["path"]! as! NSArray
                pathArr = nsPathArr.map {
                    return $0 as! String
                }
            }
            catch {
                print(error)
            }
            let positionArr = pathArr.map {
                return Position(rawValue: $0) ?? .A01
            }
            
            completionHandler(positionArr)
        }
    }
    
}

