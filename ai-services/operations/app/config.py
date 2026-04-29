from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    OPENAI_API_KEY: str = ""
    OPENAI_MODEL: str = "gpt-4.1-nano"
    OPENAI_ANALYSIS_MODEL: str = "gpt-4.1-mini"  # 매장진단/신메뉴 전용
    ORDER_SERVICE_URL: str = "http://order-service:8083"
    MENU_SERVICE_URL: str = "http://menu-service:8082"
    CONGESTION_LOW: int = 5
    CONGESTION_HIGH: int = 15
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    QUALITY_CACHE_TTL: int = 3600   # 1시간
    SUGGEST_CACHE_TTL: int = 21600  # 6시간

    class Config:
        env_file = ".env"


settings = Settings()
