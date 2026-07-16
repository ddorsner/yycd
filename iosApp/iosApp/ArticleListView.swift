import SwiftUI
import Shared

struct ArticleListView: View {
    let repository: YYCDRepository
    let location: Location
    
    @State private var posts: [WordPressPost] = []
    @State private var isLoading = true
    @State private var currentPage: Int32 = 1
    @State private var hasMorePages = true
    @State private var isFetchingMore = false
    @State private var contactDetail: IdentifiableLocationDetail? = nil
    @State private var locationDetail: LocationDetail? = nil
    
    
    var body: some View {
        Group {
            if isLoading {
                ProgressView("Loading newsletters...")
            } else {
                List {
                    ForEach(posts, id: \.id) { post in
                        NavigationLink(destination: ArticleDetailView(post: post)) {
                            PostRowView(post: post)
                        }
                        .onAppear {
                            if post.id == posts.last?.id && hasMorePages {
                                Task { await loadMorePosts() }
                            }
                        }
                    }
                    
                    if isFetchingMore {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                    }
                }
            }
        }
        .navigationTitle(location.name)
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    if let detail = locationDetail {
                        contactDetail = IdentifiableLocationDetail(id: detail.id, detail: detail)
                    } else {
                        Task {
                            await loadLocationDetail()
                            if let detail = locationDetail {
                                contactDetail = IdentifiableLocationDetail(id: detail.id, detail: detail)
                            }
                        }
                    }
                }) {
                    Image(systemName: "person.crop.circle")
                }
            }
        }
        .sheet(item: $contactDetail) { item in
            ContactView(location: item.detail)
        }
        .task {
            await loadPosts()
            await loadLocationDetail()
        }
    }
    
    private func loadPosts() async {
        do {
            let result = try await repository.getPostsForLocation(
                locationId: Int32(location.id),
                page: 1
            )
            posts = result
            hasMorePages = result.count == 10
            currentPage = 1
            isLoading = false
        } catch {
            print("Error loading posts: \(error)")
            isLoading = false
        }
    }
    
    private func loadMorePosts() async {
        guard !isFetchingMore && hasMorePages else { return }
        isFetchingMore = true
        do {
            let nextPage = currentPage + 1
            let result = try await repository.getPostsForLocation(
                locationId: Int32(location.id),
                page: nextPage
            )
            posts.append(contentsOf: result)
            hasMorePages = result.count == 10
            currentPage = nextPage
        } catch {
            print("Error loading more posts: \(error)")
        }
        isFetchingMore = false
    }
    
    private func loadLocationDetail() async {
        do {
            print("Loading detail for locationId: \(location.id)")
            locationDetail = try await repository.getLocationDetail(locationId: Int32(location.id))
            print("Loaded detail: \(locationDetail?.name ?? "nil"), phone: \(locationDetail?.phone ?? "nil")")
        } catch {
            print("Error loading location detail: \(error)")
        }
    }
}

struct PostRowView: View {
    let post: WordPressPost
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(post.title)
                .font(.headline)
                .lineLimit(2)
            
            if !post.excerpt.isEmpty {
                Text(post.excerpt)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(3)
            }
            
            Text(post.date)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}
struct IdentifiableLocationDetail: Identifiable {
    let id: Int32
    let detail: LocationDetail
}
