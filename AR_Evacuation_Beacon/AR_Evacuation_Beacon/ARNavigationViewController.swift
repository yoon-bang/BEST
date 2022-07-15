//
//  ARNavigationViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/10/22.
//

import UIKit
import ARKit
import CoreLocation



class ARNavigationViewController: UIViewController, ARSCNViewDelegate {

    @IBOutlet weak var sceneView: ARSCNView!
    @IBOutlet weak var startButton: UIButton!
    
    var navStartPosition: SCNVector3?
    var navEndPosition: SCNVector3?
    
    let configuration = ARWorldTrackingConfiguration()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.sceneView.debugOptions = [ARSCNDebugOptions.showWorldOrigin, ARSCNDebugOptions.showFeaturePoints]
        self.sceneView.showsStatistics = true
        configuration.worldAlignment = .gravityAndHeading
        self.sceneView.session.run(configuration)
        self.sceneView.delegate = self
        
        if let currentFrame = self.sceneView.session.currentFrame {
                
            var translation = matrix_identity_float4x4
            translation.columns.3.z = -0.5
            let transform = simd_mul(currentFrame.camera.transform, translation)
                
            let anchor = ARAnchor(transform: transform)
            self.sceneView.session.add(anchor: anchor)
        }
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        sceneView.session.pause()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }
    
    func generateSphereNode() -> SCNNode {
        let sphere = SCNSphere(radius: 0.05)
        let sphereNode = SCNNode()
        sphereNode.position.y += Float(sphere.radius)
        sphereNode.geometry = sphere
        return sphereNode
    }
    
    func renderer(_ renderer: SCNSceneRenderer, willRenderScene scene: SCNScene, atTime time: TimeInterval) {
        
//        guard let pointOfView = sceneView.pointOfView else {return}
        //이렇게 하면 휴대폰의 현재 포지션 백터를 얻고, 카메라의 디렉션을 얻을 수 있다. 이렇게 하면 카메라 앞에 노드를 만들 수 있다.
//        let transform = pointOfView.transform
//        let orientation = SCNVector3(-transform.m31, -transform.m32, -transform.m33)
//        let location = SCNVector3(transform.m41, transform.m42, transform.m43)
//
//        let currentPositionOfCamera = orientation + location
//
//        print(currentPositionOfCamera)
        
        
        
        DispatchQueue.main.async {
            
//            if self.startButton.isHighlighted {
//                for i in 1..<4 {
//                    let sphereNode = SCNNode(geometry: SCNSphere(radius: 0.1))
//                    sphereNode.position = SCNVector3(Double(i)*0.5, 0, 0)
//                    self.sceneView.scene.rootNode.addChildNode(sphereNode)
//                    sphereNode.geometry?.firstMaterial?.diffuse.contents = UIColor.red
//                }
//                // position.x 가 음수면 왼쪽으로, 양수면 오른쪽에찍힘
//                // position.y 가 음수면 아래로 내려감, 양수면 위로 올라감
//                // position.z 가 음수면 앞으로, 양수면 뒤로
//            }
        }
    }
    
}

func +(left: SCNVector3, right: SCNVector3) -> SCNVector3 {
    return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)
}
