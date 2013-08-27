package com.lucidchart.open.nark.models

object DynamicAlertTagModel extends DynamicAlertTagModel
class DynamicAlertTagModel extends AppModel with AlertTagModel {
	override val table = "dynamic_alert_tags"
}