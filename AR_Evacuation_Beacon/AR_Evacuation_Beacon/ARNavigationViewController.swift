//
//  ARNavigationViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/10/22.
//

import UIKit
import ARKit

class ARNavigationViewController: UIViewController, ARSCNViewDelegate {

    @IBOutlet weak var sceneView: ARSCNView!
    @IBOutlet weak var startButton: UIButton!
    
    
    let configuration = ARWorldTrackingConfiguration()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.sceneView.debugOptions = [ARSCNDebugOptions.showWorldOrigin, ARSCNDebugOptions.showFeaturePoints]
        self.sceneView.showsStatistics = true
        self.sceneView.session.run(configuration)
        self.sceneView.delegate = self
//        let currentNode = SCNNode(geometry: SCNNode(radius:0.1))
    }
    
    func renderer(_ renderer: SCNSceneRenderer, willRenderScene scene: SCNScene, atTime time: TimeInterval) {
        DispatchQueue.main.async {
            if self.startButton.isHighlighted {
                let sphereNode = SCNNode(geometry: SCNSphere(radius: 0.1))
                sphereNode.position = SCNVector3(0,0,0)
                self.sceneView.scene.rootNode.addChildNode(sphereNode)
                sphereNode.geometry?.firstMaterial?.diffuse.contents = UIColor.red
            }
        }
    }
    

    
}
