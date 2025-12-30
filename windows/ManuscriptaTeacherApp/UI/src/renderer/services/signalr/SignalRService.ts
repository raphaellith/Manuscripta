import * as signalR from "@microsoft/signalr";

class SignalRService {
    private connection: signalR.HubConnection;

    constructor() {
        this.connection = new signalR.HubConnectionBuilder()
            .withUrl("http://localhost:5000/hub") // Adjust URL as needed based on Main app configuration
            .withAutomaticReconnect()
            .configureLogging(signalR.LogLevel.Information)
            .build();
    }

    public async startConnection(): Promise<void> {
        try {
            if (this.connection.state === signalR.HubConnectionState.Disconnected) {
                await this.connection.start();
                console.log("SignalR Connection Started.");
            }
        } catch (err) {
            console.error("Error while starting SignalR connection: " + err);
            // Retry logic could be added here
            setTimeout(() => this.startConnection(), 5000);
        }
    }

    public stopConnection(): void {
        if (this.connection.state !== signalR.HubConnectionState.Disconnected) {
            this.connection.stop().then(() => console.log("SignalR Connection Stopped."));
        }
    }

    // Generic method to register handlers
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    public on(methodName: string, newMethod: (...args: any[]) => void): void {
        this.connection.on(methodName, newMethod);
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    public off(methodName: string, method: (...args: any[]) => void): void {
        this.connection.off(methodName, method);
    }
}

const signalRService = new SignalRService();
export default signalRService;
