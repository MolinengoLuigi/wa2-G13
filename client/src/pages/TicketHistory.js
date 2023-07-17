import { useEffect, useState } from "react";
import API from "../API";
import { useLocation } from "react-router-dom";
import { Card } from "react-bootstrap";
import time from "../lib/time";

const STATUS_TRADUCTIONS={
    'in_progress': 'IN PROGRESS',
    'open':'OPEN',
    'closed':'CLOSED',
    'reopened':'REOPENED',
    'resolved':'RESOLVED',
}

function TicketHistory(props){
    const [history,setHistory]=useState([]);
    const location=useLocation();
    useEffect(()=>{
        const getHistory=async ()=>{
            try {
                if(!location.state) throw new Error({status:0,detail:"No id provided"});
                const history=await API.getTicketHistory(location.state);
                setHistory(history);
            } catch (error) {
                setHistory([]);
            }
        }
        getHistory();
    },[])
    return(
        <div className="ticket-history-container">
            {history.map(h=>(
                <Card key={h.historyId} className="ticket-history-card my-2">
                    <Card.Body>
                        {h.oldStatus!==h.newStatus && "From "+STATUS_TRADUCTIONS[h.oldStatus]+" to "+STATUS_TRADUCTIONS[h.newStatus]+" : "+time.format(h.dateTime)}
                        {h.oldStatus===h.newStatus && STATUS_TRADUCTIONS[h.newStatus]+" : "+time.format(h.dateTime)}
                    </Card.Body>
                </Card>
            ))}
        </div>
    );
}

export default TicketHistory;