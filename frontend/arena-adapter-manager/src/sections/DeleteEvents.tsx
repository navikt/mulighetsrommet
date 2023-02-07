import {Section} from "../components/Section";
import {Button, Input, Select} from "@chakra-ui/react";
import {deleteEvents, replayEvent, replayEvents} from "../core/api";
import {useState} from "react";
import {arenatabeller} from "./ReplayEvents";

function DeleteEvents() {
    const [arenaIds, setArenaIds] = useState<string>("");
    const [table, setTable] = useState<string>(arenatabeller[0]);

    return (
        <Section headerText="Delete Events" loadingText={"Laster"} isLoading={false}>
            <Select
                value={table}
                onChange={({currentTarget}) => {
                    setTable(currentTarget.value);
                }}
            >
                {arenatabeller.map((tabell) => (
                    <option key={tabell} value={tabell}>
                        {tabell}
                    </option>
                ))}
            </Select>
            <Input
                placeholder="Arena id"
                value={arenaIds}
                onChange={({currentTarget}) => {
                    setArenaIds(currentTarget.value);
                }}
            />
            <Button onClick={() => deleteEvents(table, arenaIds)}>Delete Events</Button>
        </Section>
    );
}

export default DeleteEvents;
