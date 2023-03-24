import API from '../API';
import {Button, Form} from "react-bootstrap";
import React, {useState} from "react";

import Profile from "../profile"

function EditProfile() {
    const [email, setEmail] = useState('');
    const [name, setName] = useState('');
    const [surname, setSurname] = useState('');

    function editProfile() {
        try {
            const profile = API.getUserInfo(email);
            if (!profile) {
                alert("User with this email doesn't exist!")
            } else {
                const profile = new Profile(email, name, surname);
                API.updateProfile(email, profile)
                alert("User successfully added!")
            }
        } catch (error) {
            alert(error);
        }
    }

    return (
        <div className="edit-profile">
            <h3>Edit profile</h3>
            <Form onSubmit={e => e.preventDefault()}>
                <Form.Group className="mb-3" controlId="formBasicEmail">
                    <Form.Label>Email address</Form.Label>
                    <Form.Control type="email" placeholder="Email" onChange={e => setEmail(e.target.value)}/>
                </Form.Group>

                <Form.Group className="mb-3" controlId="formBasicName">
                    <Form.Label>Name</Form.Label>
                    <Form.Control type="text" placeholder="Name" onChange={e => setName(e.target.value)}/>
                </Form.Group>

                <Form.Group className="mb-3" controlId="formBasicSurname">
                    <Form.Label>Surname</Form.Label>
                    <Form.Control type="text" placeholder="Surname" onChange={e => setSurname(e.target.value)}/>
                </Form.Group>

                <Button variant="primary" type="submit" onClick={() => editProfile()}>
                    Add profile
                </Button>
            </Form>
        </div>
    );

}

export {EditProfile}