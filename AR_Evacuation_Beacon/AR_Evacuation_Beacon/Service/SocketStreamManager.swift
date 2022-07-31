//
//  SocketManager.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

import Foundation

protocol PathDelegate: NSObject {
    func received(path: Path)
}

class SocketStreamManager: NSObject, StreamDelegate {
    
    static let shared = SocketStreamManager()
    
    private override init() { }
    
    private var hostAddress = "146.148.59.28"
    private var hostPort = 12000
    var inputStream: InputStream!
    var outputStream: OutputStream!
    weak var delegate: PathDelegate?
    
    
    func connectSocket() {
        setupNetworkConnection()
    }
    
    func stopSocketSession() {
        inputStream.close()
        outputStream.close()
    }
    
    func sendLocation(location: String) {
        
        let data = location.data(using: .utf8)!
        
        data.withUnsafeBytes{
            guard let pointer = $0.baseAddress?.assumingMemoryBound(to: UInt8.self) else {
                print("Error sending location")
                return
            }
            outputStream.write(pointer, maxLength: data.count)
        }

    }
    
    func writeWithUnsafeBytes(using data: Data) {
        data.withUnsafeBytes { unsafeBufferPointer in
            guard let buffer = unsafeBufferPointer.baseAddress?.assumingMemoryBound(to: UInt8.self) else {
                NSLog("error while writing chat")
                return
            }
            outputStream?.write(buffer, maxLength: data.count)
        }
    }
    
    func readPath(stream: InputStream) {
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: 1000)
        
        while stream.hasBytesAvailable {
            guard let numberOfBytesRead = inputStream?.read(buffer, maxLength: 1000) else { return }
            
            if numberOfBytesRead < 0,
               let error = stream.streamError {
                NSLog(error.localizedDescription)
                break
            }
            
            if let path = processedMessageString(buffer: buffer, length: numberOfBytesRead) {
                delegate?.received(path: path)
            }
            
        }
    }
    
    private func processedMessageString(buffer: UnsafeMutablePointer<UInt8>, length: Int) -> Path? {
        guard var stringArray = String(bytesNoCopy: buffer, length: length, encoding: .utf8, freeWhenDone: true)?.components(separatedBy: "|") else {
            return nil
        }

        let path = stringArray[0].split(separator: " ")
            .map { String($0)}
            .filter { $0 != "" }
            .map { Position(rawValue: $0) ?? .unknown }
        
        let fireCell = stringArray[1].split(separator: " ")
            .map { String($0)}
            .filter { $0 != "" }
            .map { Position(rawValue: $0) ?? .unknown }
        
        let firePredictedCell = stringArray[2].split(separator: " ")
            .map { String($0)}
            .filter { $0 != "" }
            .map { Position(rawValue: $0) ?? .unknown }
        
        let conjestionCell = stringArray[3].split(separator: " ")
            .map { String($0)}
            .filter { $0 != "" }
            .map { Position(rawValue: $0) ?? .unknown }
        
        return Path(path: path, conjestionCell: conjestionCell, fireCell: fireCell, firePredictedCell: firePredictedCell)
    }
    
    
    private func setupNetworkConnection() {
        
        var readStream: Unmanaged<CFReadStream>?
        var writeStream: Unmanaged<CFWriteStream>?
        
        CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault, hostAddress as CFString, UInt32(hostPort), &readStream, &writeStream)
        
        inputStream = readStream!.takeRetainedValue()
        outputStream = writeStream!.takeRetainedValue()
        
        inputStream.delegate = self
        
        inputStream.schedule(in: .current, forMode: .common)
        outputStream.schedule(in: .current, forMode: .common)
        
        inputStream.open()
        outputStream.open()
        
    }
    
    func stream(_ aStream: Stream, handle eventCode: Stream.Event) {
        switch eventCode {
        case .hasBytesAvailable:
            print("new message Received")
            readPath(stream: aStream as! InputStream)
        case .endEncountered:
            print("new message Received")
        case .errorOccurred:
            print("error occurred")
        case .hasSpaceAvailable:
            print("has space available")
        case .openCompleted:
            print("open completed")
        default:
            print("some other event ...")
        }
    }
    
    func stopSession() {
        inputStream.close()
        outputStream.close()
    }
    
}


