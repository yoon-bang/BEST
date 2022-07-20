//
//  LabelTableViewCell.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/13/22.
//

import UIKit

class LabelTableViewCell: UITableViewCell {
    
    static let identifier = "LabelTableViewCell"
    static let nibName = "LabelTableViewCell"

    @IBOutlet weak var estimationLabel: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

    }
    
}
