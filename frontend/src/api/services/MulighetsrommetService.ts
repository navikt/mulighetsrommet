/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Innsatsgruppe } from '../models/Innsatsgruppe';
import type { Tiltaksgjennomforing } from '../models/Tiltaksgjennomforing';
import type { Tiltakstype } from '../models/Tiltakstype';
import type { UnsavedTiltakstype } from '../models/UnsavedTiltakstype';
import type { CancelablePromise } from '../core/CancelablePromise';
import { request as __request } from '../core/request';

export class MulighetsrommetService {

    /**
     * @returns Innsatsgruppe Array of innsatsgrupper.
     * @throws ApiError
     */
    public static getInnsatsgrupper(): CancelablePromise<Array<Innsatsgruppe>> {
        return __request({
            method: 'GET',
            path: `/api/innsatsgrupper`,
        });
    }

    /**
     * @returns Tiltakstype Array of tiltakstyper.
     * @throws ApiError
     */
    public static getTiltakstyper(): CancelablePromise<Array<Tiltakstype>> {
        return __request({
            method: 'GET',
            path: `/api/tiltakstyper`,
        });
    }

    /**
     * @returns Tiltakstype The created tiltakstype
     * @throws ApiError
     */
    public static createTiltakstype({
        requestBody,
    }: {
        requestBody: UnsavedTiltakstype,
    }): CancelablePromise<Tiltakstype> {
        return __request({
            method: 'POST',
            path: `/api/tiltakstyper`,
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * @returns Tiltakstype The specified tiltakstype.
     * @throws ApiError
     */
    public static getTiltakstype({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Tiltakstype> {
        return __request({
            method: 'GET',
            path: `/api/tiltakstyper/${id}`,
            errors: {
                404: `The specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns Tiltakstype The updated tiltakstype.
     * @throws ApiError
     */
    public static updateTiltakstype({
        id,
        requestBody,
    }: {
        /** ID **/
        id: number,
        requestBody: Tiltakstype,
    }): CancelablePromise<Tiltakstype> {
        return __request({
            method: 'PUT',
            path: `/api/tiltakstyper/${id}`,
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                404: `The specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns any The delete was successful.
     * @throws ApiError
     */
    public static deleteTiltakstype({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<any> {
        return __request({
            method: 'DELETE',
            path: `/api/tiltakstyper/${id}`,
            errors: {
                404: `The specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns Tiltaksgjennomforing Tiltaksgjennomføringer for the specified tiltakstype.
     * @throws ApiError
     */
    public static getTiltaksgjennomforingerByTiltakstype({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request({
            method: 'GET',
            path: `/api/tiltakstyper/${id}/tiltaksgjennomforinger`,
        });
    }

    /**
     * @returns Tiltaksgjennomforing Array of tiltaksgjennomføringer.
     * @throws ApiError
     */
    public static getTiltaksgjennomforinger(): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksgjennomforinger`,
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
        return __request({
            method: 'GET',
            path: `/api/tiltaksgjennomforinger/${id}`,
            errors: {
                404: `The specified tiltaksgjennomføring was not found.`,
            },
        });
    }

}