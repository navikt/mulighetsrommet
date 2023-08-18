import { MouseEvent } from 'react';

interface DialogEventDetails {
    dialogId?: string;
    aktivitetId?: string;
}

export const byttTilDialogFlate = ({
    event,
    dialogId,
    fnr,
}: {
    event: MouseEvent;
    fnr: string;
    dialogId: string;
}) => {
    event.preventDefault();
    window.history.pushState('', 'Dialog', getDialogLenke({ fnr, dialogId }));
    window.dispatchEvent(
        new CustomEvent<DialogEventDetails>('visDialog', {
            detail: {
                dialogId,
                aktivitetId: undefined,
            },
        })
    );
};

export const getDialogLenke = ({
    dialogId,
    fnr,
}: {
    fnr: string;
    dialogId: string;
}) => {
    return `/${fnr}/${dialogId}`;
};