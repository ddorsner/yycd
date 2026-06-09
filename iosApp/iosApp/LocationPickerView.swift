/*
* Copyright (c) 2026 Dabbled Studios
* Created by Dameion Dorsner
*/


import SwiftUI
import Shared

struct LocationPickerView: View {
    let repository: YYCDRepository
    @State private var locations: [Location] = []
    @State private var isLoading = true
    
    var body: some View {
        NavigationView {
            Group {
                if isLoading {
                    ProgressView("Loading locations...")
                } else {
                    List(locations, id: \.id) { location in
                        NavigationLink(destination: ArticleListView(
                            repository: repository,
                            location: location
                        )) {
                            Text(location.name)
                                .font(.body)
                                .padding(.vertical, 4)
                        }
                    }
                }
            }
            .navigationTitle("Select a Location")
            .task {
                await loadLocations()
            }
        }
    }
    
    private func loadLocations() async {
        do {
            let result = try await repository.getLocations()
            locations = result
            isLoading = false
        } catch {
            print("Error loading locations: \(error)")
            isLoading = false
        }
    }
}
