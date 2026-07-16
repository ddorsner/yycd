/*
* Copyright (c) 2026 Dabbled Studios
* Created by Dameion Dorsner
*/

import SwiftUI
import Shared

struct ContactView: View {
    let location: LocationDetail
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            List {
                if !location.phone.isEmpty {
                    Section("Phone") {
                        Button(action: { dial(location.phone) }) {
                            Label("Call \(location.phone)", systemImage: "phone.fill")
                        }
                        Button(action: { text(location.phone) }) {
                            Label("Text \(location.phone)", systemImage: "message.fill")
                        }
                    }
                }

                if !location.email.isEmpty {
                    Section("Email") {
                        Button(action: { email(location.email) }) {
                            Label("Email \(location.email)", systemImage: "envelope.fill")
                        }
                    }
                }

                if location.latitude != 0.0 && location.longitude != 0.0 {
                    Section("Directions") {
                        Button(action: { openMaps() }) {
                            Label("Get Directions", systemImage: "map.fill")
                        }
                    }
                }
            }
            .navigationTitle("Contact \(location.name)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func dial(_ phone: String) {
        if let url = URL(string: "tel://\(phone.filter { $0.isNumber })") {
            UIApplication.shared.open(url)
        }
    }

    private func text(_ phone: String) {
        if let url = URL(string: "sms:\(phone.filter { $0.isNumber })") {
            UIApplication.shared.open(url)
        }
    }

    private func email(_ address: String) {
        if let url = URL(string: "mailto:\(address)") {
            UIApplication.shared.open(url)
        }
    }

    private func openMaps() {
        let url = URL(string: "maps://?q=\(location.name)&ll=\(location.latitude),\(location.longitude)")!
        UIApplication.shared.open(url)
    }
}
