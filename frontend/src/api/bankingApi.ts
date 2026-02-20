import { http } from "./http";

export type MeResp = {
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
    iban: string;
    balance: string; // arriva come JSON number/string; lo gestiamo come string per sicurezza
};

export type TransferReq = {
    toIban: string;
    amount: number;
    causal: string;
};

export type TransferResp = {
    id: string;
    createdAt: string;
    newBalance: string;
};

export type TransferItem = {
    id: string;
    toIban: string;
    amount: string;
    createdAt: string;
};

export type MovementItem = {
    id: string;
    direction: "IN" | "OUT";
    amount: string;
    causal: string;
    createdAt: string;
    senderName: string;
    senderIban: string;
    recipientName: string;
    recipientIban: string;
};

export async function movements(): Promise<MovementItem[]> {
    const { data } = await http.get<MovementItem[]>("/api/banking/movements");
    return data;
}

export async function me(): Promise<MeResp> {
    const { data } = await http.get<MeResp>("/api/banking/me");
    return data;
}

export async function transfer(req: TransferReq): Promise<TransferResp> {
    const { data } = await http.post<TransferResp>("/api/banking/transfer", req);
    return data;
}

export async function latestTransfers(): Promise<MovementItem[]> {
    const { data } = await http.get<MovementItem[]>("/api/banking/transfers");
    return data;
}