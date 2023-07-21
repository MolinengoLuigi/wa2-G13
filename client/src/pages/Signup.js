import { useState, useContext, useEffect } from "react";
import { Form, Button, Alert, Container, Row, Spinner } from "react-bootstrap";
import { useNavigate } from 'react-router-dom';
import "../styles/Loading.css";
import "../styles/Signup.css";
import API from "../API";
import RegistrationData from "../registrationData";
import UserContext from "../context/UserContext";

function SignupPage() {
    const { user } = useContext(UserContext);
    const navigate = useNavigate();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [email, setEmail] = useState('');
    const [name, setName] = useState('');
    const [surname, setSurname] = useState('');
    const [error, setError] = useState(false);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (user.logged) {
            navigate('/home');
        }
    }, [user.logged, navigate]);


    if (!user.logged) {
        if (loading) {
            return (
                <Container fluid>
                    <Row>
                        <Spinner animation="border" variant="dark" className="spin-load" size="lg" />
                    </Row>
                </Container>
            );
        } else {
            return (
                <div className="container-sign-up-parent">
                    <div className="container-sign-up">
                        <h1 className="container-sign-up-h1">Signup</h1>
                        <Form className="container-sign-up-form"
                            onSubmit={async e => {
                                e.preventDefault();
                                e.stopPropagation();
                                setLoading(true)
                                try {
                                    await API.signup(new RegistrationData(username, password, email, name, surname));
                                    window.alert("User successfully created!")
                                    setLoading(false)
                                    navigate('/login');
                                } catch (error) {
                                    console.log(error)
                                    setError(true);
                                    setLoading(false)
                                }
                            }}>
                            <Form.Group className="mb-3">
                                <Form.Label style={{ fontWeight: "bolder" }}>Username : </Form.Label>
                                <Form.Control type="text" placeholder="insert username" name="username" required
                                    value={username}
                                    onChange={e => setUsername(e.target.value)} />
                            </Form.Group>
                            <Form.Group className="mb-3" controlId="formBasicPassword">
                                <Form.Label style={{ fontWeight: "bolder" }}>Password : </Form.Label>
                                <Form.Control type="password" placeholder="insert password" name="password" required
                                    onChange={p => setPassword(p.target.value)} />
                            </Form.Group>
                            <Form.Group className="mb-3">
                                <Form.Label style={{ fontWeight: "bolder" }}>Email : </Form.Label>
                                <Form.Control type="text" placeholder="insert email" name="email" required value={email}
                                    onChange={e => setEmail(e.target.value)} />
                            </Form.Group>
                            <Form.Group className="mb-3">
                                <Form.Label style={{ fontWeight: "bolder" }}>Name : </Form.Label>
                                <Form.Control type="text" placeholder="insert name" name="name" required value={name}
                                    onChange={e => setName(e.target.value)} />
                            </Form.Group>
                            <Form.Group className="mb-3">
                                <Form.Label style={{ fontWeight: "bolder" }}>Surname : </Form.Label>
                                <Form.Control type="text" placeholder="insert surname" name="surname" required
                                    value={surname}
                                    onChange={e => setSurname(e.target.value)} />
                            </Form.Group>
                            <Button variant="success" type="submit">Submit</Button>
                            {error ? <Alert className="my-3" variant="danger">Something went wrong!</Alert> : <></>}
                        </Form>
                    </div>
                </div>
            );
        }
    } else {
        return null;
    }
}

export default SignupPage