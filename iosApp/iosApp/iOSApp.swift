import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        let supabaseUrl = Bundle.main.object(forInfoDictionaryKey: "SUPABASE_URL") as? String ?? ""
        let supabasePublishableKey = Bundle.main.object(forInfoDictionaryKey: "SUPABASE_PUBLISHABLE_KEY") as? String ?? ""
        MainViewControllerKt.doInitKoin(supabaseUrl: supabaseUrl, supabasePublishableKey: supabasePublishableKey)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    MainViewControllerKt.doHandleDeepLink(url: url.absoluteString)
                }
        }
    }
}