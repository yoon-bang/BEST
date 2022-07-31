//
//  ARNavigationViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/10/22.
//

import UIKit
import ARKit
import CoreLocation

let modelNames: [String] = ["beacon4_ios"]
let beaconNumlist: [Int] = [4]
let beaconNum: Int = 4
let fileName: String = "ios_clf_data4A03"
let features = ["001","002","003","004","005","006","007","008","009","010",
                "011","012","013","014","015","016","017","018","019","020",
                "021", "022"]
let direction = false

final class ARNavigationViewController: UIViewController, ARSCNViewDelegate, CLLocationManagerDelegate {
    
    var map = [[Int]](repeating: [1,1,1,1,1], count: 5)
    
    @IBOutlet weak var sceneView: ARSCNView!
    
    private var map2Dview: UIView = UIView()
    private var bannerView: UIView = UIView()
    private var bannerLabel: UILabel = UILabel()
    private var indoorLocationManager = IndoorLocationManager(mode: .real)
    private var map2DViewController = Map2DViewController()
    private let mapContentScrollView: UIScrollView = {
        let scrollView = UIScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.backgroundColor = .clear
        scrollView.bounces = false
        scrollView.showsVerticalScrollIndicator = true
        scrollView.showsHorizontalScrollIndicator = true
        return scrollView
    }()
    private var arrow = SCNNode()
    private var heading: Double = 360
    private var directionDegree: Float = 0
    private var path: Path = Path()
    private var bannerText: String = NavigationDirection.forward.description {
        didSet {
            self.bannerLabel.text = bannerText
        }
    }
    
    let locationManager: CLLocationManager = {
        $0.requestWhenInUseAuthorization()
        $0.startUpdatingHeading()
        return $0
    }(CLLocationManager())
    
    let configuration = ARWorldTrackingConfiguration()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.initScene()
        self.initARSession()
        self.set2DNavigationView()
        self.setBannerView()
        locationManager.delegate = self
        arrow = generateArrowNode()
        self.sceneView.scene.rootNode.addChildNode(arrow)
        
        // Add Observer
        NotificationCenter.default.addObserver(self, selector: #selector(movenotification(_:)), name: .movePosition, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(getPath(_:)), name: .path, object: nil)
    }
    
    @objc private func getPath(_ noti: Notification) {
        guard let path = noti.object as? Path else {return}
        self.path = path
        self.bannerText = "Path Changed"
    }

}


// MARK: - AR Session Management (ARSCNViewDelegate)

extension ARNavigationViewController {
    
    func initARSession() {
        configuration.worldAlignment = .gravityAndHeading
        self.sceneView.session.run(configuration)
    }
    
    func resetARSession() {
        let config = sceneView.session.configuration as! ARWorldTrackingConfiguration
        sceneView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        self.heading = newHeading.trueHeading
        NotificationCenter.default.post(name: .changeArrowAngle, object: heading)
    }
    
}

// MARK: - Scene Management

extension ARNavigationViewController {
    
    func initScene() {
        self.sceneView.delegate = self
        sceneView.debugOptions = [
//            ARSCNDebugOptions.showFeaturePoints,
//            ARSCNDebugOptions.showWorldOrigin
        ]
        
    }
    
    func renderer(_ renderer: SCNSceneRenderer, updateAtTime time: TimeInterval) {
        
        guard let pointOfView = sceneView.pointOfView else {return}
        let transform = pointOfView.transform
        let orientation = SCNVector3(-transform.m31, -transform.m32, -transform.m33)
        let location = SCNVector3(transform.m41, transform.m42, transform.m43)
        let currentPositionOfCamera = location + orientation
        
        DispatchQueue.main.async { [self] in
            //TODO: if path is not ready {return}
            self.arrow.position = SCNVector3(x: currentPositionOfCamera.x, y: currentPositionOfCamera.y + 0.1, z: currentPositionOfCamera.z)
            self.arrow.eulerAngles = SCNVector3(x: 0, y:VectorService.changeDirection(degree: directionDegree), z: 0)
            changeBannerText(degree: VectorService.changeDirection(degree: directionDegree), heading: heading)
        }
        
    }
    
}

// MARK: - Create SCNNode
extension ARNavigationViewController {
    
    private func generateArrowNode() -> SCNNode {
        
        let vertexCount = 48;
        let vertexs: [Float] = [ -1.4923, 1.1824, 2.5000, -6.4923, 0.000, 0.000, -1.4923, -1.1824, 2.5000, 4.6077, -0.5812, 1.6800, 4.6077, -0.5812, -1.6800, 4.6077, 0.5812, -1.6800, 4.6077, 0.5812, 1.6800, -1.4923, -1.1824, -2.5000, -1.4923, 1.1824, -2.5000, -1.4923, 0.4974, -0.9969, -1.4923, 0.4974, 0.9969, -1.4923, -0.4974, 0.9969, -1.4923, -0.4974, -0.9969 ];
        
        let facecount = 13;
        let faces: [CInt] = [  3, 4, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 0, 1, 2, 3, 4, 5, 6, 7, 1, 8, 8, 1, 0, 2, 1, 7, 9, 8, 0, 10, 10, 0, 2, 11, 11, 2, 7, 12, 12, 7, 8, 9, 9, 5, 4, 12, 10, 6, 5, 9, 11, 3, 6, 10, 12, 4, 3, 11 ];
        
        let vertexData  = NSData(
            bytes: vertexs,
            length: MemoryLayout<Float>.size * vertexCount
        )
        
        let vertexSource = SCNGeometrySource(data: vertexData as Data,
                                             semantic: .vertex,
                                             vectorCount: vertexCount,
                                             usesFloatComponents: true,
                                             componentsPerVector: 3,
                                             bytesPerComponent: MemoryLayout<Float>.size,
                                             dataOffset: 0,
                                             dataStride: MemoryLayout<Float>.size * 3)
        
        let polyIndexCount = 61;
        let indexPolyData  = NSData( bytes: faces, length: MemoryLayout<CInt>.size * polyIndexCount )
        
        let element = SCNGeometryElement(data: indexPolyData as Data,
                                          primitiveType: .polygon,
                                          primitiveCount: facecount,
                                          bytesPerIndex: MemoryLayout<CInt>.size)
        
        let geometry = SCNGeometry(sources: [vertexSource], elements: [element])
        
        let material = geometry.firstMaterial!
        
        material.diffuse.contents = UIColor(red: 0.14, green: 0.82, blue: 0.95, alpha: 1.0)
        material.lightingModel = .lambert
        material.transparency = 1.00
        material.transparencyMode = .dualLayer
        material.fresnelExponent = 1.00
        material.reflective.contents = UIColor(white:0.00, alpha:1.0)
        material.specular.contents = UIColor(white:0.00, alpha:1.0)
        material.shininess = 1.00
        
        //Assign the SCNGeometry to a SCNNode, for example:
        let arrow = SCNNode()
        arrow.geometry = geometry
        arrow.scale = SCNVector3(0.05, 0.05, 0.05)
        arrow.eulerAngles = SCNVector3(x: 0, y: Float(Float(heading - 90).degreesToRadians), z: 0)
        arrow.name = "arrow"
        arrow.geometry?.firstMaterial?.diffuse.contents = UIColor.systemBlue
        
        return arrow
    }
    
    
    private func generateSphereNode() -> SCNNode {
        let sphere = SCNSphere(radius: 0.2)
        let sphereNode = SCNNode()
        sphereNode.position.y += Float(sphere.radius)
        sphereNode.geometry = sphere
        sphereNode.geometry?.firstMaterial?.diffuse.contents = UIColor.red
        return sphereNode
    }
    
}

// MARK: - UI configuration

extension ARNavigationViewController {
    
    private func setBannerView() {
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(bannerView)
        bannerView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        bannerView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        bannerView.topAnchor.constraint(equalTo: view.topAnchor, constant: 40).isActive = true
        bannerView.heightAnchor.constraint(equalToConstant: 80).isActive = true
        bannerView.backgroundColor = .lightGray.withAlphaComponent(0.7)
        
        bannerLabel.translatesAutoresizingMaskIntoConstraints = false
        bannerView.addSubview(bannerLabel)
        bannerLabel.centerXAnchor.constraint(equalTo: bannerView.centerXAnchor).isActive = true
        bannerLabel.centerYAnchor.constraint(equalTo: bannerView.centerYAnchor).isActive = true
        bannerLabel.text = "WAIT"
        bannerLabel.font = .systemFont(ofSize: 50, weight: .bold)
        bannerLabel.textColor = .green
        
        
    }
    
    private func set2DNavigationView() {
        view.addSubview(mapContentScrollView)
        mapContentScrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        mapContentScrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        mapContentScrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        mapContentScrollView.heightAnchor.constraint(equalToConstant: view.frame.height / 2.5).isActive = true
        setMap2dViewController()
    }
    
    private func setMap2dViewController() {
        guard let map2dView = map2DViewController.view else {return}
        map2dView.translatesAutoresizingMaskIntoConstraints = false
        self.map2Dview = map2dView
        mapContentScrollView.addSubview(self.map2Dview)
        
        // constraint
        self.map2Dview.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
        self.map2Dview.heightAnchor.constraint(equalTo: view.heightAnchor).isActive = true
        
        self.map2Dview.leadingAnchor.constraint(equalTo: mapContentScrollView.leadingAnchor).isActive = true
        self.map2Dview.trailingAnchor.constraint(equalTo: mapContentScrollView.trailingAnchor).isActive = true
        self.map2Dview.topAnchor.constraint(equalTo: mapContentScrollView.topAnchor).isActive = true
        self.map2Dview.bottomAnchor.constraint(equalTo: mapContentScrollView.bottomAnchor).isActive = true
    }
    
    //Observer
    @objc private func movenotification(_ noti: Notification) {
        guard let userLocation = noti.object as? Position else {return}
        mapContentScrollView.scroll(to: map2DViewController.annotationView.currentPoint)
        
        // 1.path가 들어왔다.
        guard !path.path.isEmpty else {return}
        // 2.현재위치를 파악한다.
        guard let index = path.path.firstIndex(of: userLocation) else {return}
        // 마지막 path가 아니라면
        if index < path.path.count - 1 {
            // 다음꺼의 거리와 각도를 찾기
            let start = VectorService.transformCellToCGPoint(cellname: path.path[index])
            let end = VectorService.transformCellToCGPoint(cellname:path.path[index+1])
            let vector = VectorService.vectorBetween2Points(from: start, to: end)
            
            // get angle
            directionDegree = vector.angle
            // get dist
            let dist = vector.dist
            
            // generate sphere to next cell
            // 1 cell = 36cm
            let newnode = generateSphereNode()
            sceneView.scene.rootNode.addChildNode(newnode)
            newnode.position = SCNVector3(x: arrow.position.x, y: arrow.position.y, z: arrow.position.z - (Float(dist) / 10 * 0.36) + 1.0)
            
        } else {
            bannerText = "Safely Exit"
            let alert = UIAlertController(title: "tt", message: nil, preferredStyle: .alert)
            self.present(alert, animated: true)
        }
        
    }
    
    private func changeBannerText(degree: Float, heading: Double) {
        print(degree)
        let pointDirection = VectorService.headingToDirection(degree: degree)
        let headingDirection = VectorService.headingToDirection(degree: Float(heading))
        // 남동북서 0123 음수면 오른쪽으로 양수면
        // 현재 동쪽을 가리킬때,
        print("pointing ",pointDirection)
        print(headingDirection)
        
        let direction = pointDirection.rawValue - headingDirection.rawValue
        if direction == 0 {
            self.bannerText = NavigationDirection.forward.description
        } else if direction == -1 {
            self.bannerText = NavigationDirection.right.description
        } else if direction == 1 {
            self.bannerText = NavigationDirection.left.description
        } else {
            self.bannerText = NavigationDirection.backward.description
        }
        
    }

}

func +(left: SCNVector3, right: SCNVector3) -> SCNVector3 {
    return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)
}

extension Float {
    
    var degreesToRadians: Float { return Float(self) * .pi/180}
}

extension Int {
    
    var degreesToRadians: Double { return Double(self) * .pi/180}
}

extension UIScrollView {
    func scroll(to point: CGPoint) {
        let y = point.y - (self.frame.height / 2) < 0 ? 0 : point.y - (self.frame.height / 2)
        self.setContentOffset(CGPoint(x: 0, y: y), animated: true)
    }
}
