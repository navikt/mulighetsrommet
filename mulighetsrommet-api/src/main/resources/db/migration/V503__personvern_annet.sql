INSERT INTO personopplysning (value, title, help_text, sort_key) VALUES
(
    'ANNET',
    'Annet (skal kun benyttes etter avtale med Direktoratet)',
    'Dette kan for eksempel være aktuelt i forbindelse med avklaring på skip, hvor det kan være nødvendig å behandle følgende opplysningstyper: kles og skostørrelse, opplysninger om innvilgelse eller nektet innvilgelse av visum, passopplysninger, opplysninger om foretrukket kontaktperson (nødkontakt)',
    26
);

ALTER TABLE avtale_personopplysning ADD COLUMN beskrivelse TEXT;
