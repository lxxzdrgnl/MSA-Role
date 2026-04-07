from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    OPENAI_API_KEY: str = ""
    OPENAI_MODEL: str = "gpt-4o-mini"
    MENU_SERVICE_URL: str = "http://menu-service:8082"
    CHROMA_PERSIST_PATH: str = "/data/chroma"
    EMBEDDING_MODEL: str = "jhgan/ko-sroberta-multitask"

    class Config:
        env_file = ".env"


settings = Settings()
