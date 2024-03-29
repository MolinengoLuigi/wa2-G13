import './App.css';
import 'bootstrap/dist/css/bootstrap.min.css';

import UserContext from './context/UserContext';
import Header from './components/Header';
import { useContext, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import {
    HomepagePage,
    LoginPage,
    PurchasesPage,
    TicketPage,
    ExpertsPage,
    SignupPage,
    CreateExpertPage,
    UserInfoPage,
    ModifyExpertPage,
    TicketHistory
} from './pages';
import TokenManager from './TokenManager';
import { ProductTable } from "./components/ProductTable";
import { ManageTicketForm } from "./components/ManageTicketForm";
import { TicketList } from './components';
import ScrollToTopButton from './components/ScrollToTopButton';
import SectorsContext from "./context/SectorsContext";


function CheckHeader(props) {
    const location = useLocation();

    if (location.pathname.includes("/login") || location.pathname.includes("/signup")) {
        return null; // Hide the menu on the login page
    }

    return <Header handleLogout={props.handleLogout} logged={props.logged} name={props.name} />;
}

function CheckContentContainer(props) {
    const location = useLocation();

    if (location.pathname.includes("/login") || location.pathname.includes("/signup")) {
        return props.children;
    }

    return <div className="content-container">{props.children}</div>;
}

function App() {
    const tokenManager = TokenManager();
    const [user, setUser] = useState(tokenManager.retrieveUser());
    const [logged, setLogged] = useState(tokenManager.amILogged());
    const navigate = useNavigate();
    let { sectors } = useContext(SectorsContext);

    const handleLogout = () => {
        tokenManager.removeAuthToken();
        setUser(tokenManager.clearUser());
        setLogged(false);
        sectors = []
        navigate("/login");
    }

    return (
        <UserContext.Provider value={{ user, setUser }}>
            <CheckHeader handleLogout={handleLogout} logged={logged} name={user.username} />
            <CheckContentContainer>
                <Routes>
                    <Route index element={<Navigate replace to='/home' />} />
                    <Route path="/home" element={<HomepagePage />} />
                    <Route path="/login" element={<LoginPage setLogged={setLogged} />} />
                    <Route path="/tickets"
                        element={user.logged ? (user.role === "manager" ? <TicketList /> : <TicketPage />) :
                            <Navigate to="/home" />} />
                    <Route path="/signup" element={<SignupPage setLogged={setLogged} />} />
                    <Route path="/createExpert" element={<CreateExpertPage />} />
                    <Route path="/userInfo" element={<UserInfoPage handleLogout={handleLogout} />} />
                    <Route path="/products" element={<ProductTable />} />;
                    <Route path="/purchases" element={<PurchasesPage />} />;
                    <Route path='/experts' element={<ExpertsPage />} />;
                    <Route path="/modifyExpert/:expertId" element={<ModifyExpertPage />} />
                    {user.logged && user.role === "manager" &&
                        <Route path="/tickets/manage/:ticketId" element={<ManageTicketForm />} />}
                    <Route path="/tickets/history/:ticketId" element={<TicketHistory />} />;
                </Routes>
            </CheckContentContainer>
            <ScrollToTopButton />
        </UserContext.Provider>
    );
}

export default App;
