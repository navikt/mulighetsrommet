/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Bruker } from '../models/Bruker';
import type { FrontendEvent } from '../models/FrontendEvent';
import type { Tiltaksgjennomforing } from '../models/Tiltaksgjennomforing';
import type { Tiltakskode } from '../models/Tiltakskode';
import type { Tiltakstype } from '../models/Tiltakstype';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class MulighetsrommetService {

    /**
     * @returns any Sanity query result
     * @throws ApiError
     */
    public static sanityQuery({
        query,
        fnr,
    }: {
        /** Sanity query as a Groq-string. See https://www.sanity.io/docs/groq for more information. **/
        query: string,
        /** SSN for the user which will be used to filter based on the users 'oppfølgingsenhet' **/
        fnr?: string,
    }): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/sanity',
            query: {
                'query': query,
                'fnr': fnr,
            },
        });
    }

    /**
     * @returns Tiltakstype Array of tiltakstyper.
     * @throws ApiError
     */
    public static getTiltakstyper({
        search,
        innsatsgrupper,
    }: {
        /** Search for tiltakstyper **/
        search?: string,
        /** Innsatsgruppefilter **/
        innsatsgrupper?: Array<number>,
    }): CancelablePromise<Array<Tiltakstype>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/tiltakstyper',
            query: {
                'search': search,
                'innsatsgrupper': innsatsgrupper,
            },
        });
    }

    /**
     * @returns Tiltakstype Created tiltakstype
     * @throws ApiError
     */
    public static createTiltakstype({
        requestBody,
    }: {
        requestBody?: Tiltakstype,
    }): CancelablePromise<Tiltakstype> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/tiltakstyper',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * @returns Tiltakstype the specified tiltakstype.
     * @throws ApiError
     */
    public static getTiltakstype({
        tiltakskode,
    }: {
        /** Tiltakskode **/
        tiltakskode: Tiltakskode,
    }): CancelablePromise<Tiltakstype> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/tiltakstyper/{tiltakskode}',
            path: {
                'tiltakskode': tiltakskode,
            },
            errors: {
                404: `The specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns Tiltakstype Updated tiltakstype
     * @throws ApiError
     */
    public static updateTiltakstype({
        tiltakskode,
        requestBody,
    }: {
        /** Tiltakskode **/
        tiltakskode: Tiltakskode,
        requestBody?: Tiltakstype,
    }): CancelablePromise<Tiltakstype> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/v1/tiltakstyper/{tiltakskode}',
            path: {
                'tiltakskode': tiltakskode,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * @returns Tiltaksgjennomforing An array of tiltaksgjennomføringer for specified tiltakskode.
     * @throws ApiError
     */
    public static getTiltaksgjennomforingerByTiltakskode({
        tiltakskode,
    }: {
        /** Tiltakskode **/
        tiltakskode: Tiltakskode,
    }): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/tiltakstyper/{tiltakskode}/tiltaksgjennomforinger',
            path: {
                'tiltakskode': tiltakskode,
            },
            errors: {
                404: `the specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns Tiltaksgjennomforing Array of tiltaksgjennomføringer.
     * @throws ApiError
     */
    public static getTiltaksgjennomforinger({
        search,
        innsatsgrupper,
        tiltakstyper,
    }: {
        /** Search for tiltaksgjennomforinger **/
        search?: string,
        /** Innsatsgruppefilter **/
        innsatsgrupper?: Array<number>,
        /** Tiltakstypefilter **/
        tiltakstyper?: Array<number>,
    }): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/tiltaksgjennomforinger',
            query: {
                'search': search,
                'innsatsgrupper': innsatsgrupper,
                'tiltakstyper': tiltakstyper,
            },
        });
    }

    /**
     * @returns Tiltaksgjennomforing The specified tiltaksgjennomføring.
     * @throws ApiError
     */
    public static getTiltaksgjennomforing({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Tiltaksgjennomforing> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/tiltaksgjennomforinger/{id}',
            path: {
                'id': id,
            },
            errors: {
                404: `The specified tiltaksgjennomføring was not found.`,
            },
        });
    }

    /**
     * @returns Bruker Data about the 'bruker' in the context of 'veileder'
     * @throws ApiError
     */
    public static getBrukerdata({
        fnr,
    }: {
        /** Fødselsnummer **/
        fnr: string,
    }): CancelablePromise<Bruker> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/bruker/{fnr}',
            path: {
                'fnr': fnr,
            },
            errors: {
                404: `Could not find data about 'bruker'`,
            },
        });
    }

    /**
     * @returns FrontendEvent Logged frontend event
     * @throws ApiError
     */
    public static logFrontendEvent({
        requestBody,
    }: {
        requestBody?: FrontendEvent,
    }): CancelablePromise<FrontendEvent> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/logger/event',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Logging to frontend failed`,
            },
        });
    }

}