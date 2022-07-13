//
//  SocketManager.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

import Foundation

class SocketStreamManager: NSObject, StreamDelegate {
    
    private var hostAddress = "146.148.59.28"
    private var hostPort = 12000
    var inputStream: InputStream!
    var outputStream: OutputStream!
    
    func connectSocket() {
        setupNetworkConnection()
    }
    
    func stopSocketSession() {
        inputStream.close()
        outputStream.close()
    }
    
    func sendLocation(location: String) {
        
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
        let availableBytes = UnsafeMutablePointer<UInt8>.allocate(capacity: 1000)
        
        while stream.hasBytesAvailable {
            guard let numberOfBytesRead = inputStream?.read(availableBytes, maxLength: 1000) else { return }
            
            if numberOfBytesRead < 0,
               let error = stream.streamError {
                NSLog(error.localizedDescription)
                break
            }
            
            
//            try? delegate?.convertDataToTexts(buffer: availableBytes, length: numberOfBytesRead)
        }
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
