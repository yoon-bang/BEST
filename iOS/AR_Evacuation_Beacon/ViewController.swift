//
//  ViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/9/22.
//

import UIKit

class ViewController: UIViewController {
    
    let socketManager = SocketManager()

    override func viewDidLoad() {
        super.viewDidLoad()
        socketManager.connectSocket()
    }
    
    
    
    


}

