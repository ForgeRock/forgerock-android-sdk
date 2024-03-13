//
//  SampleKMMIOSAppApp.swift
//  SampleKMMIOSApp
//
//  Created by jey periyasamy on 3/14/24.
//

import SwiftUI
import logging

@main
struct SampleKMMIOSAppApp: App {
    
    var log = KmLog(tag: "AppDelegate")
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
    
    init() {
      logger()
    }
    
    func logger() {
        log.debug(tag: "AppDelegte::class") {
            "I am in Kotlin multiplatform"
        }
    }
}
