/*
* Copyright (c) 2026 Dabbled Studios
* Created by Dameion Dorsner
*/


import SwiftUI
import Shared

struct ContentView: View {
    @State private var splashData: SplashData? = nil
    @State private var isLoading = true
    @State private var showLocationPicker = false
    
    private let repository = YYCDRepository()
    
    var body: some View {
        if showLocationPicker {
            LocationPickerView(repository: repository)
        } else {
            splashView
        }
    }
    
    var splashView: some View {
        ZStack {
            Color.white.ignoresSafeArea()
            
            VStack(spacing: 24) {
                if isLoading {
                    ProgressView("Loading...")
                } else {
                    if let splash = splashData {
                        // Logo image
                        if !splash.titleUrl.isEmpty {
                            AsyncImage(url: URL(string: splash.titleUrl)) { image in
                                image
                                    .resizable()
                                    .scaledToFit()
                                    .frame(maxWidth: 280)
                            } placeholder: {
                                ProgressView()
                            }
                        }
                        
                        // Splash image
                        if !splash.splashUrl.isEmpty {
                            AsyncImage(url: URL(string: splash.splashUrl)) { image in
                                image
                                    .resizable()
                                    .scaledToFit()
                                    .frame(maxWidth: .infinity)
                                    .padding(.horizontal)
                            } placeholder: {
                                ProgressView()
                            }
                        }
                        
                        // Splash text
                        if !splash.splashText.isEmpty {
                            Text(splash.splashText)
                                .font(.body)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                        }
                    }
                    
                    // Continue button
                    Button(action: { showLocationPicker = true }) {
                        Text("Select a Location")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .cornerRadius(10)
                            .padding(.horizontal)
                    }
                }
            }
            .padding()
        }
        .task {
            await loadSplashData()
        }
    }
    
    private func loadSplashData() async {
        do {
            splashData = try await repository.getSplashData()
            isLoading = false
        } catch {
            print("Error loading splash data: \(error)")
            isLoading = false
        }
    }
}
