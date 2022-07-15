//
//  BeaconCellTableViewCell.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/10/22.
//

import UIKit

class BeaconCell: UITableViewCell {
    
    static let identifier = "BeaconCell"
    
    @IBOutlet weak var idLabel: UILabel!
    @IBOutlet weak var majorLabel: UILabel!
    @IBOutlet weak var minorLabel: UILabel!
    @IBOutlet weak var rssiLabel: UILabel!
    @IBOutlet weak var filteredRssiLabel: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

}
