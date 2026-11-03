import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Navbar from './components/Navbar'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Hospitals from './pages/Hospitals'
import BloodBanks from './pages/BloodBanks'
import Donors from './pages/Donors'
import DonorDetail from './pages/DonorDetail'
import BloodUnits from './pages/BloodUnits'
import BloodRequests from './pages/BloodRequests'
import BloodRequestDetail from './pages/BloodRequestDetail'
import Dispatches from './pages/Dispatches'
import NotFound from './pages/NotFound'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/hospitals" element={<Hospitals />} />
            <Route path="/blood-banks" element={<BloodBanks />} />
            <Route path="/donors" element={<Donors />} />
            <Route path="/donors/:id" element={<DonorDetail />} />
            <Route path="/blood-units" element={<BloodUnits />} />
            <Route path="/blood-requests" element={<BloodRequests />} />
            <Route path="/blood-requests/:id" element={<BloodRequestDetail />} />
            <Route path="/dispatches" element={<Dispatches />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
