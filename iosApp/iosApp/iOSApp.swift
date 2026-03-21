import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        let supabaseUrl = Bundle.main.infoDictionary?["SUPABASE_URL"] as? String ?? ""
        let supabaseAnonKey = Bundle.main.infoDictionary?["SUPABASE_ANON_KEY"] as? String ?? ""
        KoinHelperKt.doInitKoin(supabaseUrl: supabaseUrl, supabaseAnonKey: supabaseAnonKey)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
