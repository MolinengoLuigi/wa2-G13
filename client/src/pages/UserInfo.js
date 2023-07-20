import {useContext, useEffect, useState} from "react";
import UserContext from "../context/UserContext";
import API from "../API";
import "../styles/Loading.css";
import "../styles/UserInfo.css";
import {useNavigate} from 'react-router-dom';
import {Col, Container, Row, Form, Button, Alert, Spinner} from "react-bootstrap";
import Profile from "../profile";
import Manager from "../manager";
import Expert from "../expert";
import {MdAddCircleOutline, MdRemoveCircleOutline} from 'react-icons/md'
import SectorsContext from "../context/SectorsContext";

function UserInfoPage(props){
    const {user, setUser}= useContext(UserContext);
    const {sectors, setSectors}= useContext(SectorsContext);
    const navigate = useNavigate();
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [name, setName] = useState('');
    const [surname, setSurname] = useState('');
    const [error, setError] = useState('');
    const [showMenu, setShowMenu] = useState(false);
    const [loading, setLoading]= useState(false);

    useEffect(() => {
        if (!user.logged) {
            navigate('/login');
        }else{
            fetchUserInfo().catch((error) => {
                console.error(error);
            });
        }
    }, [user.logged]);

    const togglePopupMenu = () => {
        setShowMenu(!showMenu);
    };

    const fetchUserInfo = async () => {
        try {
            let userInfo;

            if (user.role === 'customer') {
                userInfo = await API.getProfileInfo(user.id);
            } else if (user.role === 'expert') {
                userInfo = await API.getExpertInfo(user.id);

                let sectorsList;
                sectorsList = await API.getSectorsOfExpert(user.id);
                const updatedSectors={
                    ...sectors,
                    sectors: sectorsList
                }
                setSectors(updatedSectors);
            } else if (user.role === 'manager'){
                userInfo = await API.getManagerInfo(user.id);
            }
            const updatedUser = {
                ...user,
                username: userInfo.username,
                email: userInfo.email,
                name: userInfo.name,
                surname: userInfo.surname,
            };
            setUser(updatedUser);
        } catch (error) {
            console.error(error);
        }
    };

    if (user.logged) {
        if (loading) {
            return (
                <Container fluid>
                    <Row>
                        <Spinner animation="border" variant="dark" className="spin-load" size="lg"/>
                    </Row>
                </Container>
            );
        } else {
            return (
                <div className="user-info-container">
                    <Container className="user-info">
                        <h1 className="user-info-h1">User Info</h1>
                        <Row className="user-info-row">
                            <Col xs={12} sm={4}>
                                <strong>Username:</strong>
                            </Col>
                            <Col xs={12} sm={8}>
                                <span>{user.username}</span>
                            </Col>
                        </Row>
                        <Row className="user-info-row">
                            <Col xs={12} sm={4}>
                                <strong>Email:</strong>
                            </Col>
                            <Col xs={12} sm={8}>
                                <span>{user.email}</span>
                            </Col>
                        </Row>
                        <Row className="user-info-row">
                            <Col xs={12} sm={4}>
                                <strong>Name:</strong>
                            </Col>
                            <Col xs={12} sm={8}>
                                <span>{user.name}</span>
                            </Col>
                        </Row>
                        <Row className="user-info-row">
                            <Col xs={12} sm={4}>
                                <strong>Surname:</strong>
                            </Col>
                            <Col xs={12} sm={8}>
                                <span>{user.surname}</span>
                            </Col>
                        </Row>
                        {user.role==="expert" && (
                            <Row className="user-info-row">
                                <Col xs={12} sm={4}>
                                    <strong>Sectors:</strong>
                                </Col>
                                <Col xs={12} sm={8}>
                                    <span>{sectors.sectors===[] ? "" : sectors.sectors.join(", ")}</span>
                                </Col>
                            </Row>
                        )
                        }
                    </Container>

                    <Container className="modify-user-info">
                        <h1 className="mx-auto">Modify User Info</h1>
                        <button className="open-menu-button" onClick={togglePopupMenu}>
                            {showMenu ? <MdRemoveCircleOutline className="button-icon"/> :
                                <MdAddCircleOutline className="button-icon"/>}
                        </button>
                        {showMenu && (
                            <div className="popup-menu">
                                <Form onSubmit={async e => {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    setLoading(true)
                                    try {
                                        if (user.role === 'customer') {
                                            await API.modifyProfile(user.id, new Profile(user.id, username, email, name, surname))
                                        } else if (user.role === 'expert') {
                                            await API.modifyExpert(user.id, new Expert(user.id, username, email, name, surname))
                                        } else if (user.role === 'manager') {
                                            await API.modifyManager(user.id, new Manager(user.id, username, email, name, surname))
                                        }
                                        window.alert("Modifications correctly saved. LOGIN is needed!")
                                        setUsername('')
                                        setEmail('')
                                        setName('')
                                        setSurname('')
                                        await API.logout()
                                        setLoading(false)
                                        navigate("/login")
                                    } catch (error) {
                                        setError(error);
                                        setLoading(false);
                                    }
                                }}>
                                    <Form.Group className="mb-3">
                                        <Form.Label style={{fontWeight: "bolder"}}>Username : </Form.Label>
                                        <Form.Control type="text"
                                                      placeholder="insert the new username or confirm the old one"
                                                      name="username" required value={username}
                                                      onChange={e => setUsername(e.target.value)}/>
                                    </Form.Group>
                                    <Form.Group className="mb-3">
                                        <Form.Label style={{fontWeight: "bolder"}}>Email : </Form.Label>
                                        <Form.Control type="email"
                                                      placeholder="insert the new email or confirm the old one"
                                                      name="email" required
                                                      onChange={p => setEmail(p.target.value)}/>
                                    </Form.Group>
                                    <Form.Group className="mb-3">
                                        <Form.Label style={{fontWeight: "bolder"}}>Name : </Form.Label>
                                        <Form.Control type="text"
                                                      placeholder="insert the new name or confirm the old one"
                                                      name="name" required value={name}
                                                      onChange={e => setName(e.target.value)}/>
                                    </Form.Group>
                                    <Form.Group className="mb-3">
                                        <Form.Label style={{fontWeight: "bolder"}}>Surname : </Form.Label>
                                        <Form.Control type="text"
                                                      placeholder="insert the new surname or confirm the old one"
                                                      name="surname" required value={surname}
                                                      onChange={e => setSurname(e.target.value)}/>
                                    </Form.Group>
                                    <Button variant="success" type="submit">Submit</Button>
                                    {error!=="" ? <Alert className="my-3" variant="danger">Error during modification: {error.message}</Alert> : <></>}
                                </Form>
                            </div>
                        )}
                    </Container>
                </div>
            );
        }
    } else{
        return null;
    }
}
export  default UserInfoPage