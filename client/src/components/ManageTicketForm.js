import React, { useContext, useEffect, useState } from 'react';
import UserContext from "../context/UserContext";
import { useNavigate, useParams, Link } from 'react-router-dom';
import API from "../API";
import { Form, Button, Table, Modal } from 'react-bootstrap'

const OverlayAlert = ({ show, onClose, title, message }) => {
    return (
        <Modal show={show} onHide={onClose}>
            <Modal.Header closeButton>
                <Modal.Title>{title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>{message}</Modal.Body>
            <Modal.Footer>
                <Button variant="primary" onClick={onClose}>
                    Close
                </Button>
            </Modal.Footer>
        </Modal>
    );
};

function ManageTicketForm() {

    const [ticket, setTicket] = useState(null);
    const { user } = useContext(UserContext);
    const navigate = useNavigate();
    const { ticketId } = useParams();
    const [sectorExperts, setSectorExperts] = useState(null);
    const [selectedPriorityLevel, setSelectedPriorityLevel] = useState(null);
    const [selectedExpert, setSelectedExpert] = useState(null)

    const [alertMessage, setAlertMessage] = useState('');

    const [expertsNotFound, setExpertsNotFound] = useState(false);

    const [showAlert, setShowAlert] = useState(false);
    const [alertTitle, setAlertTitle] = useState('');
    async function load() {
        await API.getTicket(ticketId).then(res => {
            setTicket(res);
            if (res.expert) setSelectedExpert(res.expert.id);
            if (res.priorityLevel || res.priorityLevel === 0) setSelectedPriorityLevel(res.priorityLevel);
            API.getExpertsBySector(res.product.sector.name)
                .then(res => setSectorExperts(res))
                .catch(err => {
                    setExpertsNotFound(true);
                    setShowAlert(true);
                    setAlertTitle('Attention');
                    setAlertMessage(`No experts found of the sector: ${res.product.sector.name}`);
                    console.log(`No experts found of the sector: ${res.product.sector.name}`)
                });
        });
    }

    useEffect(() => {
        if (!user.logged && user.role !== 'manager') {
            navigate('/home');
        } else void load()
    }, [navigate, user.logged, user.role]);


    const handlePriorityChange = (event) => {
        setSelectedPriorityLevel(event.target.value);
    };

    const handleExpertChange = (event) => {
        setSelectedExpert(event.target.value);
    };

    async function handleSave(event) {
        let an_expert_is_selected = false;
        let a_priority_is_selected = false
        if (selectedExpert !== null && selectedExpert !== 'null') {
            an_expert_is_selected = true;
        }
        if (selectedPriorityLevel !== null && selectedPriorityLevel !== 'null') {
            a_priority_is_selected = true;
        }
        if (an_expert_is_selected && a_priority_is_selected) {
            if (selectedPriorityLevel !== ticket.priorityLevel?.toString()) await API.changePriorityLevel(ticketId, parseInt(selectedPriorityLevel));
            if (selectedExpert !== ticket.expert?.id.toString()) await API.changeExpert(ticketId, selectedExpert);
            if (ticket.status === 'open') await API.changeStatus(ticketId, "in_progress");
            setShowAlert(true);
            setAlertTitle('Success')
            setAlertMessage("The ticket was successfully saved")
        } else {
            event.preventDefault();
            setShowAlert(true);
            setAlertTitle('Warning');
            setAlertMessage('Select both an expert and a priority level to save the ticket, ' +
                'otherwise just click \'Cancel\' to go back to the ticket list ' +
                'without changing anything');
        }
    }

    const reformatDate = (dateString) => {
        let dateObj = new Date(dateString);

        let year = dateObj.getFullYear();
        let month = ("0" + (dateObj.getMonth() + 1)).slice(-2);
        let day = ("0" + dateObj.getDate()).slice(-2);
        let hours = ("0" + dateObj.getHours()).slice(-2);
        let minutes = ("0" + dateObj.getMinutes()).slice(-2);

        return `${year}/${month}/${day} ${hours}:${minutes}`;
    }

    if ((!ticket || !sectorExperts) && !expertsNotFound)
        return <div>Loading...</div>;
    else
        return (
            <>
                {showAlert ?
                    <OverlayAlert show={showAlert} onClose={() => {
                        alertTitle === 'Success' ? navigate('/tickets'): setShowAlert(false);
                    }} title={alertTitle} message={alertMessage}/>
                : null}
                <div className="manage-ticket-form">
                    <h4 className='text-center' style={{ marginBottom: '20px' }}>Manage the ticket with id '{ticketId}'</h4>
                    <span>{' '}</span>
                    <Table striped bordered hover>
                        <thead>
                            <tr>
                                <th>Creation Date</th>
                                <td>{reformatDate(ticket.creationDate)}</td>
                            </tr>
                            <tr>
                                <th>Status</th>
                                <td>{ticket.status}</td>
                            </tr>
                            <tr>
                                <th>Customer Email</th>
                                <td>{ticket.profile.email}</td>
                            </tr>
                            <tr>
                                <th>Customer Name</th>
                                <td>{ticket.profile.name}</td>
                            </tr>
                            <tr>
                                <th>Customer Surname</th>
                                <td>{ticket.profile.surname}</td>
                            </tr>
                            <tr>
                                <th>Customer Username</th>
                                <td>{ticket.profile.username}</td>
                            </tr>
                            <tr>
                                <th>Product Ean</th>
                                <td>{ticket.product.ean}</td>
                            </tr>
                            <tr>
                                <th>Product Brand</th>
                                <td>{ticket.product.brand}</td>
                            </tr>
                            <tr>
                                <th>Product Name</th>
                                <td>{ticket.product.name}</td>
                            </tr>
                            <tr>
                                <th>Product Sector</th>
                                <td>{ticket.product.sector.name}</td>
                            </tr>
                            <tr>
                                <th>Expert (only experts of the related sector can be assigned to the ticket):</th>
                                <td><Form.Select value={selectedExpert ? selectedExpert.toString() : "null"} onChange={handleExpertChange}>
                                    <option value="null"></option>
                                    {sectorExperts ?
                                        sectorExperts.map(exp => {
                                            return <option key={exp.id} value={exp.id.toString()}>{`${exp.name} ${exp.surname} [${exp.email} - ${exp.username}]`}</option>
                                        }) : null
                                    }
                                </Form.Select></td>
                            </tr>
                            <tr>
                                <th>Ticket Priority Level</th>
                                <td><Form.Select value={(selectedPriorityLevel || selectedPriorityLevel === 0) ? selectedPriorityLevel.toString() : "null"} onChange={handlePriorityChange}>
                                    <option value="null"></option>
                                    <option value="0">0</option>
                                    <option value="1">1</option>
                                    <option value="2">2</option>
                                    <option value="3">3</option>
                                    <option value="4">4</option>
                                </Form.Select></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td>
                                    <Button onClick={handleSave}>Save</Button>
                                    <span>{' '}</span>
                                    <Link to={'/tickets'}>
                                        <Button>Cancel</Button>
                                    </Link>
                                </td>
                            </tr>
                        </thead>
                    </Table>
                </div>
            </>
        );
}

export { ManageTicketForm }