import { Route, BrowserRouter as Router, Routes } from "react-router-dom"
// import GitHubCorner from "./components/GitHubCorner"
import Toast from "./components/toast/Toast"
import EditorPage from "./pages/EditorPage"
import HomePage from "./pages/HomePage"
import Header from "./components/Header"
import CheckMaintainability from "./pages/CheckMaintainability"
import ComplexityCalc from "./pages/ComplexityCalc"
import LineCountCalculator from "./pages/LineCountCalculator"
import HalsteadMetricCalculator from "./pages/HalsteadMetricCalculator"
import Home from "./pages/Home"

const App = () => {
    return (
        <>
            <Router>
                <div className="sticky top-0 z-50">
                    <Header />
                </div>
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/home" element={<Home/>}/>
                    <Route path="/editor/:roomId" element={<EditorPage />} />
                    <Route
                        path="/maintainabilitycal"
                        element={<CheckMaintainability />}
                    />
                    <Route path="/complexitycal" element={<ComplexityCalc />} />
                    <Route path="/loccal" element={<LineCountCalculator />} />
                    <Route
                        path="/halsteadmetriccalc"
                        element={<HalsteadMetricCalculator />}
                    />
                </Routes>
            </Router>
            <Toast /> {/* Toast component from react-hot-toast */}
            {/* <GitHubCorner /> */}
        </>
    )
}

export default App
